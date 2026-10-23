// ============================================================================
// Xtrim — la poda del CSS de Fomantic (HU-30)
//
// El bundle de Fomantic son 13 752 reglas; el proyecto usa 82 clases. Este
// script deja sólo las reglas que el sitio puede necesitar y las escribe en
// `generated/fomantic.css`, que es el fichero que el build de Angular usa como
// hoja de estilos.
//
// Se ejecuta **antes** del build (`npm run prebuild`), para que el peso que
// Angular mide —y el presupuesto que vigila— sea el del CSS final, y no el
// del bundle entero.
//
//   node scripts/purge-css.mjs
// ============================================================================

import { readFileSync, writeFileSync, mkdirSync, readdirSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import postcss from 'postcss';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const OUT_DIR = join(ROOT, 'generated');
const OUT_FILE = join(OUT_DIR, 'fomantic.css');
const SOURCE = join(ROOT, 'node_modules/fomantic-ui-css/semantic.min.css');

/** The budget of the stylesheet, in bytes (requirements.md §4.1). */
const CSS_BUDGET = 200 * 1024;

/**
 * The classes that the project can put in the DOM but that do not appear in a
 * `class="..."` attribute: the ones that arrive through a binding
 * (`[class.active]`) or that a state of Fomantic can add.
 *
 * <p>The list is short on purpose: every class that is here keeps alive the
 * rules of Fomantic that mention it. The classes that the templates write
 * literally are collected from the templates themselves.
 */
const STATE_CLASSES = [
  // Los que el proyecto pone con [class.X]
  'active', 'disabled', 'error', 'positive', 'negative', 'red', 'green', 'blue',
  'grey', 'orange', 'yellow', 'is-active', 'is-hovered', 'is-selected',
  'xtrim-selected',
  // Los estados que Fomantic estiliza y que el sitio puede tener
  'visible', 'hidden', 'loading', 'selected', 'focus', 'animating',
];

/** Walks a directory and returns the files that match a predicate. */
function walk(dir, predicate, found = []) {
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) {
      walk(path, predicate, found);
    } else if (predicate(path)) {
      found.push(path);
    }
  }
  return found;
}

/** The classes and the attributes that the templates and the code can use. */
function collectUsedMarkup() {
  const classes = new Set(STATE_CLASSES);
  const attributes = new Set();
  const sources = walk(
    join(ROOT, 'src'),
    (p) => p.endsWith('.html') || p.endsWith('.ts') || p.endsWith('.scss'),
  );

  for (const file of sources) {
    const text = readFileSync(file, 'utf8');
    // class="one two three"
    for (const match of text.matchAll(/class="([^"]*)"/g)) {
      for (const name of match[1].split(/\s+/)) {
        if (name) {
          classes.add(name);
        }
      }
    }
    // [class.one.two]
    for (const match of text.matchAll(/\[class\.([^\]]+)\]/g)) {
      for (const name of match[1].split('.')) {
        if (name) {
          classes.add(name);
        }
      }
    }
    // classList.add('one')
    for (const match of text.matchAll(/classList\.(?:add|remove|toggle|contains)\('([^']+)'\)/g)) {
      classes.add(match[1]);
    }
    // The attributes of the markup: <input type="search">, [attr.aria-label],
    // and the ones that are only in the code.
    for (const match of text.matchAll(/\b([a-zA-Z-]+)="([^"]*)"/g)) {
      attributes.add(match[1].toLowerCase());
      if (match[2]) {
        attributes.add(`${match[1].toLowerCase()}=${match[2].toLowerCase()}`);
      }
    }
    for (const match of text.matchAll(/\[attr\.([^\]]+)\]/g)) {
      attributes.add(match[1].split('.')[0].toLowerCase());
    }
  }
  return { classes, attributes };
}

/**
 * Whether a selector can match the markup of the project.
 *
 * <p>The rule is strict on purpose: a selector survives only when **all** its
 * classes are used, when its attribute selectors can be satisfied, and when
 * the negative conditions (`:not(...)`) do not forbid anything. A selector
 * that mentions a class or an attribute the site does not have can never
 * match, and its rule is dead weight — which is how the four thousand rules
 * of the emojis of Fomantic disappear.
 */
function selectorMatches(selector, used) {
  // The negative conditions: they do not forbid anything, they only exclude.
  const positive = selector.replace(/:not\([^)]*\)/g, '');

  // The attribute selectors that talk about classes.
  for (const match of selector.matchAll(/\[class([*~^$]?=)["']?([^\]"']+)["']?\]/g)) {
    // The capture is the operator and its "="; the operator is the first one.
    const operator = match[1][0];
    const value = match[2].trim();
    const satisfied = value.split(/\s+/).every((part) => {
      switch (operator) {
        case '~':
          return used.classes.has(part);
        case '*':
          return [...used.classes].some((name) => name.includes(part));
        case '^':
          return [...used.classes].some((name) => name.startsWith(part));
        case '$':
          return [...used.classes].some((name) => name.endsWith(part));
        default:
          return false;
      }
    });
    if (!satisfied) {
      return false;
    }
  }

  // The attribute selectors that talk about anything else: the attribute has
  // to be one the markup has, and the value has to be one it uses.
  for (const match of positive.matchAll(/\[([a-zA-Z-]+)(?:=["']?([^\]"']*)["']?)?\]/g)) {
    const name = match[1].toLowerCase();
    if (name === 'class') {
      continue;
    }
    if (match[2] === undefined) {
      if (!used.attributes.has(name)) {
        return false;
      }
      continue;
    }
    const value = match[2].trim().toLowerCase();
    if (!used.attributes.has(`${name}=${value}`)) {
      return false;
    }
  }

  // The classes of the selector: all of them have to be there.
  for (const match of positive.matchAll(/\.(-?[_a-zA-Z]+[_a-zA-Z0-9-]*)/g)) {
    if (!used.classes.has(match[1])) {
      return false;
    }
  }
  return true;
}

/** Removes the rules that the project can never match. */
function purge(css, used) {
  let kept = 0;
  let dropped = 0;

  const root = postcss.parse(css);
  root.walkRules((rule) => {
    const selectors = rule.selectors ?? [];
    const alive = selectors.filter((selector) => selectorMatches(selector, used));
    if (alive.length === 0) {
      rule.remove();
      dropped += 1;
      return;
    }
    kept += 1;
    if (alive.length !== selectors.length) {
      rule.selectors = alive;
    }
  });

  // The comments of the original file are noise once it is minified.
  root.walkComments((comment) => comment.remove());

  return { css: root.toString(), kept, dropped };
}

// --- the run -----------------------------------------------------------------

const used = collectUsedMarkup();
const original = readFileSync(SOURCE, 'utf8');
const { css, kept, dropped } = purge(original, used);

/**
 * The fonts of Fomantic live in the package, and the URLs of the original CSS
 * are relative to it. The generated file lives in `generated/`, so the URLs
 * are rewritten to point at the same files.
 */
const withFonts = css.replace(
  /url\((["']?)themes\/default\/assets\/([^"')]+)\1\)/g,
  'url($1../node_modules/fomantic-ui-css/themes/default/assets/$2$1)',
);

const ratio = ((withFonts.length / original.length) * 100).toFixed(1);
console.log(
  `» the stylesheet of Fomantic: ${(original.length / 1024).toFixed(1)} KB → ` +
    `${(withFonts.length / 1024).toFixed(1)} KB (${ratio}%), ${kept} rules kept, ${dropped} dropped`,
);

mkdirSync(OUT_DIR, { recursive: true });
writeFileSync(OUT_FILE, withFonts);

if (withFonts.length > CSS_BUDGET) {
  console.error(
    `✗ The stylesheet is ${(withFonts.length / 1024).toFixed(1)} KB and the budget is ` +
      `${(CSS_BUDGET / 1024).toFixed(0)} KB`,
  );
  process.exit(1);
}
