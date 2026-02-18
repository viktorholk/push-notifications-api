import { defineConfig } from 'astro/config';
import tailwind from '@astrojs/tailwind';

export default defineConfig({
  site: 'https://viktorholk.github.io',
  base: '/push-notifications-api',
  integrations: [tailwind()],
});
