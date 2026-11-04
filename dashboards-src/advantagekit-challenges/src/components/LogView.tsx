import type { Panels } from "../challenges";
import type { LogItem, LogValue, Pose, Tree } from "../lab";

const KIND_LABEL = { input: "Input", output: "Output", dashboard: "Dashboard" };

function isPose(value: unknown): value is Pose {
	return typeof value === "object" && value !== null && "x" in value && "r" in value;
}

function formatNumber(value: number): string {
	if (Number.isInteger(value)) return String(value);
	return Math.abs(value) >= 100 ? value.toFixed(0) : value.toFixed(3).replace(/0+$/, "");
}

function formatPose(pose: Pose): string {
	return `(${pose.x.toFixed(2)} m, ${pose.y.toFixed(2)} m, ${((pose.r * 180) / Math.PI).toFixed(0)}°)`;
}

function formatValue(value: LogValue): string {
	if (typeof value === "number") return formatNumber(value);
	if (typeof value === "boolean") return value ? "true" : "false";
	if (typeof value === "string") return value;
	if (isPose(value)) return formatPose(value);
	if (value.length === 0) return "[ ] (empty)";
	const items = value as (number | boolean | string | Pose)[];
	if (items.length > 5) return `[ ${items.length} values ]`;
	const shown = items.map((item) =>
		isPose(item) ? `(${item.x.toFixed(1)}, ${item.y.toFixed(1)})` : formatValue(item as LogValue),
	);
	return `[ ${shown.join(", ")} ]`;
}

/** A friendlier name for a logged type. */
function formatType(type: string): string {
	return type.replace(/^struct:/, "").replace("double", "number").replace("int64", "integer").replace("float", "number");
}

function Row({ item, showKinds }: { item: LogItem; showKinds: boolean }) {
	return (
		<tr>
			{showKinds && (
				<td>
					<span className={`kind kind-${item.kind}`}>{KIND_LABEL[item.kind]}</span>
				</td>
			)}
			<td className="log-key">
				<code>{item.key}</code>
			</td>
			<td className="log-type">
				{formatType(item.type)}
				{item.unit && ` · ${item.unit}`}
			</td>
			<td className="log-value">
				<code>{formatValue(item.value)}</code>
			</td>
		</tr>
	);
}

/**
 * What's in the log right now. The keys a challenge asks for come first, and
 * stay visible as "not in the log yet" until the student's code writes them.
 */
export function LogView({ panels, tree }: { panels: Panels; tree: Tree | undefined }) {
	const showKinds = panels.showKinds ?? false;
	const items = tree?.items ?? [];
	const hidden = (key: string) => (panels.hide ?? []).some((prefix) => key.startsWith(prefix));
	const expected = new Set(panels.log.map((entry) => `${entry.kind}:${entry.key}`));
	const find = (key: string, kind: string) => items.find((item) => item.key === key && item.kind === kind);
	const others = items.filter((item) => !expected.has(`${item.kind}:${item.key}`) && !hidden(item.key));
	const nothing = panels.log.length === 0 && others.length === 0;

	return (
		<section className="card panel">
			<span className="panel-label">Your log</span>
			{nothing ? (
				<p className="muted">Nothing's been logged yet.</p>
			) : (
				<div className="table-scroll">
					<table className="log-table">
						<tbody>
							{panels.log.map((entry) => {
								const item = find(entry.key, entry.kind);
								return item ? (
									<Row key={`${entry.kind}:${entry.key}`} item={item} showKinds={showKinds} />
								) : (
									<tr key={`${entry.kind}:${entry.key}`} className="log-missing">
										{showKinds && (
											<td>
												<span className={`kind kind-${entry.kind}`}>{KIND_LABEL[entry.kind]}</span>
											</td>
										)}
										<td className="log-key">
											<code>{entry.key}</code>
										</td>
										<td colSpan={2} className="log-type">
											not in the log yet
										</td>
									</tr>
								);
							})}
							{others.map((item) => (
								<Row key={`${item.kind}:${item.key}`} item={item} showKinds={showKinds} />
							))}
						</tbody>
					</table>
				</div>
			)}
		</section>
	);
}
