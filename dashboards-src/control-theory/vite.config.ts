import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
	plugins: [react()],
	// CodeRunner serves a dashboard from a tokenised path inside the student's
	// project, never from the site root, so every asset URL must be relative.
	base: "./",
	build: {
		// Dashboards cannot load files outside their own directory, and a single
		// bundle keeps the output easy to copy into a lesson module.
		assetsInlineLimit: 8192,
	},
});
