/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Brand red scale. Also backfills the `primary-*` classes that
        // components already reference but were never defined.
        primary: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#ef4444',
          600: '#dc2626',
          700: '#b91c1c',
          800: '#991b1b',
          900: '#7f1d1d',
          950: '#450a0a',
        },
      },
      fontFamily: {
        display: ['"Archivo Black"', 'Impact', 'Haettenschweiler', 'sans-serif'],
        sans: ['"Space Grotesk"', 'system-ui', '-apple-system', 'sans-serif'],
      },
      boxShadow: {
        // Hard, blur-free offsets — core of the maximalist/brutalist look
        brutal: '6px 6px 0 0 #000000',
        'brutal-sm': '3px 3px 0 0 #000000',
        'brutal-lg': '10px 10px 0 0 #000000',
        'brutal-xl': '14px 14px 0 0 #000000',
        'brutal-red': '6px 6px 0 0 #b91c1c',
        'brutal-red-lg': '10px 10px 0 0 #b91c1c',
        'brutal-white': '6px 6px 0 0 #ffffff',
        'brutal-white-lg': '10px 10px 0 0 #ffffff',
      },
      backgroundImage: {
        'mx-stripes': 'repeating-linear-gradient(45deg, #000000 0 10px, transparent 10px 20px)',
        'mx-stripes-red': 'repeating-linear-gradient(45deg, #b91c1c 0 10px, transparent 10px 20px)',
        'mx-stripes-white': 'repeating-linear-gradient(45deg, #ffffff 0 10px, transparent 10px 20px)',
        'mx-dots': 'radial-gradient(#000000 1.5px, transparent 1.6px)',
        'mx-dots-white': 'radial-gradient(#ffffff 1.5px, transparent 1.6px)',
        'mx-dots-red': 'radial-gradient(#b91c1c 1.5px, transparent 1.6px)',
        'mx-grid': 'linear-gradient(#000000 1px, transparent 1px), linear-gradient(90deg, #000000 1px, transparent 1px)',
      },
      keyframes: {
        marquee: {
          '0%': { transform: 'translateX(0)' },
          '100%': { transform: 'translateX(-50%)' },
        },
        wiggle: {
          '0%, 100%': { transform: 'rotate(-2deg)' },
          '50%': { transform: 'rotate(2deg)' },
        },
        blink: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.35' },
        },
      },
      animation: {
        marquee: 'marquee 24s linear infinite',
        'marquee-fast': 'marquee 12s linear infinite',
        wiggle: 'wiggle 3s ease-in-out infinite',
        blink: 'blink 1.4s step-end infinite',
      },
    },
  },
  plugins: [],
}
