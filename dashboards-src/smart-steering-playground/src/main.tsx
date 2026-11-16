import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { installDevMock } from "./coderunner/devMock";
import "./App.css";

// Inside CodeRunner, `window.coderunner` already exists before this runs.
// On the Vite dev server it does not, so fall back to sample data.
if (import.meta.env.DEV) installDevMock();

const root = document.getElementById("root");
if (root) {
	createRoot(root).render(
		<StrictMode>
			<App />
		</StrictMode>,
	);
}
