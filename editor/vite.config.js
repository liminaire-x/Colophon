import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { viteSingleFile } from 'vite-plugin-singlefile'

// Builds the whole editor into a single self-contained dist/index.html
// so the Colophon mod can serve it from one classpath resource.
// After building, copy dist/index.html to ../src/main/resources/colophon/web/index.html
export default defineConfig({
  plugins: [react(), viteSingleFile()],
  build: { outDir: 'dist', target: 'es2020' },
})
