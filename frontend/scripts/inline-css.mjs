// ============================================================================
// Xtrim — el CSS dentro del documento (HU-30)
//
// El CSS podado se mete en el documento que envía el servidor, en lugar de
// dejar un `<link rel="stylesheet">`: el navegador no hace una segunda
// petición y el documento no se recompone cuando el CSS llega.
//
// Se ejecuta **después** del build (`npm run build`).
//
//   node scripts/inline-css.mjs
// ============================================================================

import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const BROWSER = join(ROOT, 'dist/frontend/browser');
const SERVER = join(ROOT, 'dist/frontend/server');

/**
 * The documents that the server can send.
 *
 * <p>The important one is the *asset chunk*: the runtime of the SSR does not
 * read `index.server.html` from the disk, it imports it from the bundle, so
 * that is the one that has to carry the CSS.
 */
const DOCUMENTS = [
  { file: 'assets-chunks/index_server_html.mjs', dir: SERVER, javascript: true },
  { file: 'assets-chunks/index_csr_html.mjs', dir: SERVER, javascript: true },
  { file: 'index.server.html', dir: SERVER, javascript: false },
  { file: 'index.csr.html', dir: BROWSER, javascript: false },
];

const stylesheet = readdirSync(BROWSER).find((name) => /^styles-.*\.css$/.test(name));
if (!stylesheet) {
  console.error('✗ There is no stylesheet in dist/frontend/browser');
  process.exit(1);
}

const css = readFileSync(join(BROWSER, stylesheet), 'utf8');

/**
 * The URLs of the stylesheet are relative to the file that holds it. Once the
 * CSS is inside the document, they have to be relative to the **document**,
 * which is not the same thing: on `/productos/laptop-nova-14-pulgadas`,
 * `./media/font.woff2` would be looked for in `/productos/media/`. They become
 * absolute, from the root of the site.
 */
const absolute = css.replace(/url\((["']?)\.\/media\//g, 'url($1/media/');

/**
 * The CSS inside a JavaScript template literal: the backslashes of the
 * escapes of CSS (`content:"\f0ab"`) and the dollars have to be escaped, or
 * the template literal would read them.
 */
function forTemplateLiteral(text) {
  return text.replace(/\\/g, '\\\\').replace(/`/g, '\\`').replace(/\$\{/g, '\\${');
}

const link = new RegExp(`<link[^>]*rel="stylesheet"[^>]*href="${stylesheet}"[^>]*>`, 'g');

/**
 * The fonts that the text of the site uses, so that the browser asks for them
 * **before** it paints the first text (HU-30).
 *
 * <p>Without them the font is asked for when the first text is painted, and it
 * arrives after: the text is painted with the fallback and moves when Lato
 * arrives. With the preloads, the font travels in parallel with the document
 * and the text is painted with Lato from the start.
 *
 * <p>Only the two weights that the site uses (the normal and the bold) and
 * only the Latin subset: the italics are not used by any page.
 */
function fontPreloads(css) {
  const preloads = [];
  for (const block of css.matchAll(/@font-face\{([^}]*)\}/g)) {
    const declarations = block[1];
    const family = declarations.match(/font-family:([^;]+)/)?.[1]?.trim();
    const style = declarations.match(/font-style:([^;]+)/)?.[1]?.trim() ?? 'normal';
    const source = declarations.match(/url\(["']?([^"')]+\.woff2)["']?\)/)?.[1];
    // Only the Latin subset (the file names start with "LatoLatin") and only
    // the normal style: the italics are not used by any page.
    if (
      family !== 'Lato' ||
      style !== 'normal' ||
      !source ||
      !source.includes('LatoLatin')
    ) {
      continue;
    }
    const href = source.startsWith('/') ? source : `/${source.replace(/^\.\//, '')}`;
    preloads.push(
      `<link rel="preload" as="font" type="font/woff2" crossorigin href="${href}">`,
    );
  }
  return preloads;
}

for (const { file, dir, javascript } of DOCUMENTS) {
  const path = join(dir, file);
  let content;
  try {
    content = readFileSync(path, 'utf8');
  } catch {
    continue;
  }
  if (!link.test(content)) {
    console.log(`» ${file}: there is no stylesheet to inline`);
    continue;
  }
  link.lastIndex = 0;
  const style = `<style>${javascript ? forTemplateLiteral(absolute) : absolute}</style>`;
  const preloads = fontPreloads(absolute).join('');
  writeFileSync(path, content.replace(link, `${preloads}${style}`));
  console.log(
    `» ${file}: the CSS goes inside the document (${(absolute.length / 1024).toFixed(1)} KB)` +
      `${preloads ? ' and the fonts are preloaded' : ''}`,
  );
}
