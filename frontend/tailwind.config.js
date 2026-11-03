/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        flow: {
          bg: '#0c1017',
          sidebar: '#080c13',
          card: '#121824',
          cardBorder: '#1e293b',
          cardHover: '#162032',
          accent: '#3b82f6',
          success: '#10b981',
          warning: '#f59e0b',
          danger: '#ef4444',
          muted: '#64748b'
        }
      }
    },
  },
  plugins: [],
}
