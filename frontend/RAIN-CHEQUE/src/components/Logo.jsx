import React, { useId } from 'react';

/** Brand red — used for the hard offset layer (print misregistration effect). */
const ACCENT = '#b91c1c';

const WORDMARK_FONT = "'Archivo Black','Arial Black',Impact,sans-serif";

/**
 * Rain Cheque brand logo — maximalist treatment.
 *
 * A perforated ticket bearing an "R" beside the "Cheque" wordmark, drawn twice:
 * once offset in brand red, once in `currentColor`. That hard, blur-free offset
 * is the same device used by the app's panels and buttons.
 *
 * The mark inherits its main color from `currentColor`, and the ticket cutouts
 * (notches, perforation, and the "R" window) are genuinely transparent, so it
 * sits on any background. Size and color it with className, e.g.
 * "h-9 w-auto text-white" on dark, "h-12 w-auto text-black" on light.
 *
 * The wordmark uses `textLength` so its advance width is fixed regardless of
 * which font actually loads — that keeps the © and underline from colliding.
 */
const Logo = ({ className = 'h-9 w-auto', title = 'Rain Cheque' }) => {
  // useId can contain ':' which is awkward inside url(#...) references.
  const maskId = `rc-ticket-${useId().replace(/:/g, '')}`;

  const Mark = ({ fill }) => (
    <g fill={fill}>
      {/* Ticket body — cutouts punched through by the mask */}
      <rect x="26" y="26" width="118" height="48" rx="6" mask={`url(#${maskId})`} />

      {/* Wordmark */}
      <text
        x="164"
        y="66"
        fontFamily={WORDMARK_FONT}
        fontSize="42"
        textLength="150"
        lengthAdjust="spacingAndGlyphs"
      >
        Cheque
      </text>

      {/* Chunky rule under the wordmark */}
      <rect x="164" y="76" width="150" height="8" />

      {/* Copyright mark */}
      <text x="326" y="38" textAnchor="middle" fontFamily="Arial, sans-serif" fontSize="13">
        ©
      </text>
    </g>
  );

  return (
    <svg
      className={className}
      viewBox="16 18 344 82"
      role="img"
      aria-label={title}
      xmlns="http://www.w3.org/2000/svg"
    >
      <title>{title}</title>

      <mask id={maskId}>
        {/* Visible ticket area */}
        <rect x="26" y="26" width="118" height="48" rx="6" fill="white" />

        {/* Side notches (holes) */}
        <circle cx="26" cy="50" r="7" fill="black" />
        <circle cx="144" cy="50" r="7" fill="black" />

        {/* Perforation line (holes) */}
        <circle cx="54" cy="32" r="2.5" fill="black" />
        <circle cx="54" cy="41" r="2.5" fill="black" />
        <circle cx="54" cy="50" r="2.5" fill="black" />
        <circle cx="54" cy="59" r="2.5" fill="black" />
        <circle cx="54" cy="68" r="2.5" fill="black" />

        {/* "R" window (hole) with the letter painted back in */}
        <rect x="66" y="30" width="42" height="40" rx="6" fill="black" />
        <text
          x="87"
          y="61"
          textAnchor="middle"
          fontFamily={WORDMARK_FONT}
          fontSize="32"
          fill="white"
        >
          R
        </text>
      </mask>

      {/* Hard offset layer, in brand red */}
      <g transform="translate(6,6)" aria-hidden="true">
        <Mark fill={ACCENT} />
      </g>

      {/* Main layer, inherits text color */}
      <Mark fill="currentColor" />
    </svg>
  );
};

export default Logo;
