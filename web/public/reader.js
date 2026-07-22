'use strict'

/**
 * @callback OnError
 * @param {string} err - error.
 */

/**
 * @callback OnTrack
 * @param {RTCTrackEvent} evt - track event.
 */

/**
 * @typedef Conf
 * @type {object}
 * @property {string} url - absolute URL of the WHEP endpoint.
 * @property {string} user - username.
 * @property {string} pass - password.
 * @property {string} token - token.
 * @property {OnError} onError - called when there's an error.
 * @property {OnTrack} onTrack - called when there's a track available.
 */

/** WebRTC/WHEP reader. */
class MediaMTXWebRTCReader {
  /**
   * Create a MediaMTXWebRTCReader.
   * @param {Conf} conf - configuration.
   */
  constructor(conf) {
    this.conf = conf
    this.pc = null
    this.offerData = null
    this.sessionUrl = null
    this.queuedCandidates = []
    this.state = 'running'
    this.#start()
  }

  /**
   * Close the reader and all its resources.
   */
  close() {
    this.state = 'closed'

    if (this.pc !== null) {
      this.pc.close()
    }
  }

  static #unquoteCredential(v) {
    return JSON.parse(`"${v}"`)
  }

  static #linkToIceServers(links) {
    return (links !== null)
      ? links.split(', ').map((link) => {
        const m = link.match(/^<(.+?)>; rel="ice-server"(; username="(.*?)"; credential="(.*?)"; credential-type="password")?/i)
        const ret = {
          urls: [m[1]],
        }

        if (m[3] !== undefined) {
          ret.username = this.#unquoteCredential(m[3])
          ret.credential = this.#unquoteCredential(m[4])
          ret.credentialType = 'password'
        }

        return ret
      })
      : []
  }

  static #parseOffer(sdp) {
    const ret = {
      iceUfrag: '',
      icePwd: '',
      medias: [],
    }

    for (const line of sdp.split('\r\n')) {
      if (line.startsWith('m=')) {
        ret.medias.push(line.slice('m='.length))
      } else if (ret.iceUfrag === '' && line.startsWith('a=ice-ufrag:')) {
        ret.iceUfrag = line.slice('a=ice-ufrag:'.length)
      } else if (ret.icePwd === '' && line.startsWith('a=ice-pwd:')) {
        ret.icePwd = line.slice('a=ice-pwd:'.length)
      }
    }

    return ret
  }

  static #generateSdpFragment(od, candidates) {
    const candidatesByMedia = {}
    for (const candidate of candidates) {
      const mid = candidate.sdpMLineIndex
      if (candidatesByMedia[mid] === undefined) {
        candidatesByMedia[mid] = []
      }
      candidatesByMedia[mid].push(candidate)
    }

    let frag = `a=ice-ufrag:${od.iceUfrag}\r\n` +
      `a=ice-pwd:${od.icePwd}\r\n`

    let mid = 0

    for (const media of od.medias) {
      if (candidatesByMedia[mid] !== undefined) {
        frag += `m=${media}\r\n` +
          `a=mid:${mid}\r\n`

        for (const candidate of candidatesByMedia[mid]) {
          frag += `a=${candidate.candidate}\r\n`
        }
      }
      mid++
    }
    return frag
  }

  #handleError(err) {
    if (this.state === 'running') {
      if (this.pc !== null) {
        this.pc.close()
        this.pc = null
      }

      this.offerData = null

      if (this.sessionUrl !== null) {
        fetch(this.sessionUrl, {
          method: 'DELETE',
        })
        this.sessionUrl = null
      }

      // 调用用户传入的 onError 回调
      if (this.conf.onError !== undefined) {
        this.conf.onError(err)
      }
    }
  }

  #start() {
    this.#requestICEServers()
      .then((iceServers) => this.#setupPeerConnection(iceServers))
      .then((offer) => this.#sendOffer(offer))
      .then((answer) => this.#setAnswer(answer))
      .catch((err) => {
        this.#handleError(err.toString())
      })
  }

  #authHeader() {
    if (this.conf.user !== undefined && this.conf.user !== '') {
      const credentials = btoa(`${this.conf.user}:${this.conf.pass}`)
      return { Authorization: `Basic ${credentials}` }
    }
    if (this.conf.token !== undefined && this.conf.token !== '') {
      return { Authorization: `Bearer ${this.conf.token}` }
    }
    return {}
  }

  #requestICEServers() {
    return fetch(this.conf.url, {
      method: 'OPTIONS',
      headers: {
        ...this.#authHeader(),
      },
    })
      .then((res) => MediaMTXWebRTCReader.#linkToIceServers(res.headers.get('Link')))
  }

  #setupPeerConnection(iceServers) {
    if (this.state !== 'running') {
      throw new Error('closed')
    }

    this.pc = new RTCPeerConnection({
      iceServers,
      encodedInsertableStreams: this.conf.insertableStreams === true,
      // https://webrtc.org/getting-started/unified-plan-transition-guide
      // sdpSemantics: 'unified-plan',
    })

    const direction = 'recvonly'
    this.pc.addTransceiver('video', { direction })
    // this.pc.addTransceiver('audio', { direction });

    this.pc.onicecandidate = (evt) => this.#onLocalCandidate(evt)
    this.pc.onconnectionstatechange = () => this.#onConnectionState()
    this.pc.ontrack = (evt) => this.#onTrack(evt)

    return this.pc.createOffer()
      .then((offer) => {
        this.offerData = MediaMTXWebRTCReader.#parseOffer(offer.sdp)
        return this.pc.setLocalDescription(offer)
          .then(() => offer.sdp)
      })
  }

  #sendOffer(offer) {
    if (this.state !== 'running') {
      throw new Error('closed')
    }

    return fetch(this.conf.url, {
      method: 'POST',
      headers: {
        ...this.#authHeader(),
        'Content-Type': 'application/sdp',
      },
      body: offer,
    })
      .then((res) => {
        switch (res.status) {
          case 201:
            break
          case 404:
            throw new Error('stream not found')
          case 400:
            return res.json().then((e) => { throw new Error(e.error) })
          default:
            throw new Error(`bad status code ${res.status}`)
        }
        const locationHeader = res.headers.get('location')
        if (locationHeader !== null) {
          // SRS 返回完整路径 /rtc/v1/whip/?... 或简短 session id
          if (locationHeader.startsWith('/') || locationHeader.startsWith('http')) {
            // 完整路径：直接使用（Vite ^/rtc 代理会转发）
            this.sessionUrl = locationHeader.startsWith('http')
              ? new URL(locationHeader).pathname + new URL(locationHeader).search
              : locationHeader
          } else {
            // 简单 session id：拼在原始 URL 后面
            this.sessionUrl = this.conf.url + '/' + locationHeader
          }
        } else {
          // Fallback if location header is missing or not exposed via CORS
          this.sessionUrl = this.conf.url
        }

        return res.text()
      })
  }

  #setAnswer(answer) {
    if (this.state !== 'running') {
      throw new Error('closed')
    }

    return this.pc.setRemoteDescription(new RTCSessionDescription({
      type: 'answer',
      sdp: answer,
    }))
      .then(() => {
        if (this.state !== 'running') {
          return
        }

        if (this.queuedCandidates.length !== 0) {
          this.#sendLocalCandidates(this.queuedCandidates)
          this.queuedCandidates = []
        }
      })
  }

  #onLocalCandidate(evt) {
    // SRS 5.0 在 SDP Answer 中已内嵌 ICE Candidate，不需要 trickle ICE PATCH。
    // 跳过发送，避免本地产生大量 pending 请求或线上 404。
  }

  #sendLocalCandidates(candidates) {
    fetch(this.sessionUrl, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/trickle-ice-sdpfrag',
        'If-Match': '*',
      },
      body: MediaMTXWebRTCReader.#generateSdpFragment(this.offerData, candidates),
    })
      .then((res) => {
        switch (res.status) {
          case 204:
            break
          case 404:
            // SRS 已在 SDP Answer 中内嵌了 ICE Candidate，trickle ICE 不是必须的。
            // 线上 Nginx 未代理 /rtc/v1/whip/ 时会返回 404，不应该因此杀死整个连接！
            console.warn('Trickle ICE PATCH returned 404 (ignored, SDP-embedded ICE is sufficient)')
            break
          default:
            throw new Error(`bad status code ${res.status}`)
        }
      })
      .catch((err) => {
        // 仅打印警告，不触发 handleError 断连
        console.warn('Trickle ICE PATCH failed (non-fatal):', err.toString())
      })
  }

  #onConnectionState() {
    if (this.state !== 'running') {
      return
    }

    // "closed" can arrive before "failed" and without
    // the close() method being called at all.
    // It happens when the other peer sends a termination
    // message like a DTLS CloseNotify.
    if (this.pc.connectionState === 'failed' ||
      this.pc.connectionState === 'closed'
    ) {
      this.#handleError('peer connection closed')
    }
  }

  #onTrack(evt) {
    if (this.conf.onTrack !== undefined) {
      this.conf.onTrack(evt)
    }
  }
}

window.MediaMTXWebRTCReader = MediaMTXWebRTCReader
