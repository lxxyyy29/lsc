import { spawn } from 'node:child_process'

const forwardedArgs = process.argv.slice(2).filter((arg) => arg !== '--runInBand')
const vitestArgs = ['vitest', 'run', ...forwardedArgs]

const child = spawn(process.platform === 'win32' ? 'npx.cmd' : 'npx', vitestArgs, {
  stdio: 'inherit',
  shell: false,
  env: {
    ...process.env,
    UNI_INPUT_DIR: process.env.UNI_INPUT_DIR || '.'
  }
})

child.on('exit', (code) => {
  process.exit(code ?? 1)
})

child.on('error', (error) => {
  console.error(error)
  process.exit(1)
})
