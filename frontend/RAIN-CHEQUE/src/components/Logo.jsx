import React from 'react';

/**
 * Rain Cheque brand logo.
 *
 * Single-color SVG lockup on a TRANSPARENT background: a perforated ticket
 * bearing an "R" next to the "Cheque" wordmark and a copyright mark.
 *
 * The whole mark inherits the current text color via `currentColor`, and the
 * ticket cutouts (perforation, notches, and the window around the "R") are
 * genuinely transparent, so it works on any background. Set the color and size
 * with className, e.g. "h-9 w-auto text-white" on a dark surface or
 * "h-12 w-auto text-black" on a light one.
 */
const Logo = ({ className = 'h-9 w-auto', title = 'Rain Cheque' }) => (
  <svg
    className={className}
    viewBox="0 0 340 100"
    role="img"
    aria-label={title}
    fill="currentColor"
    xmlns="http://www.w3.org/2000/svg"
  >
    <title>{title}</title>

    <mask id="rc-ticket-mask">
      {/* Visible ticket area */}
      <rect x="26" y="26" width="118" height="48" rx="10" fill="white" />
      {/* Side notches (holes) */}
      <circle cx="26" cy="50" r="6" fill="black" />
      <circle cx="144" cy="50" r="6" fill="black" />
      {/* Perforation line (holes) */}
      <circle cx="54" cy="34" r="2" fill="black" />
      <circle cx="54" cy="42" r="2" fill="black" />
      <circle cx="54" cy="50" r="2" fill="black" />
      <circle cx="54" cy="58" r="2" fill="black" />
      <circle cx="54" cy="66" r="2" fill="black" />
      {/* "R" window (hole) */}
      <rect x="66" y="30" width="40" height="40" rx="9" fill="black" />
      {/* "R" painted back in */}
      <text
        x="86"
        y="61"
        textAnchor="middle"
        fontFamily="'Arial Rounded MT Bold','Segoe UI',Arial,sans-serif"
        fontSize="34"
        fontWeight="800"
        fill="white"
      >R</text>
    </mask>

    {/* Ticket body, masked so cutouts show through as transparent */}
    <rect
      x="26"
      y="26"
      width="118"
      height="48"
      rx="10"
      mask="url(#rc-ticket-mask)"
    />

    {/* Wordmark */}
    <text
      x="164"
      y="65"
      fontFamily="'Arial Rounded MT Bold','Segoe UI',Arial,sans-serif"
      fontSize="42"
      fontWeight="700"
      letterSpacing="-1"
    >Cheque</text>

    {/* Copyright mark */}
    <text
      x="326"
      y="38"
      textAnchor="middle"
      fontFamily="Arial,sans-serif"
      fontSize="12"
    >©</text>
  </svg>
);

export default Logo;
