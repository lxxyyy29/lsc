import { copyFileSync, existsSync, mkdirSync, readdirSync, rmSync, statSync } from 'node:fs'
import { join } from 'node:path'

const source = 'static'
const target = 'dist/build/h5/static'

if (!existsSync(source)) {
  process.exit(0)
}

rmSync(target, { recursive: true, force: true })

function copyDirectory(from, to) {
  mkdirSync(to, { recursive: true })

  for (const entry of readdirSync(from)) {
    const sourcePath = join(from, entry)
    const targetPath = join(to, entry)
    const stats = statSync(sourcePath)

    if (stats.isDirectory()) {
      copyDirectory(sourcePath, targetPath)
      continue
    }

    if (stats.isFile()) {
      copyFileSync(sourcePath, targetPath)
    }
  }
}

copyDirectory(source, target)
