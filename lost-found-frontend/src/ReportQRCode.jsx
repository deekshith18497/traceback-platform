import { useState } from "react";

export default function ReportQRCode({ item }) {
    const [expanded, setExpanded] = useState(false);
    const [message, setMessage] = useState("");

    if (!item?.id || !item?._type) {
        return null;
    }

    const reportUrl = new URL(window.location.href);
    reportUrl.search = "";
    reportUrl.searchParams.set("report", String(item.id));
    reportUrl.searchParams.set("type", item._type);
    reportUrl.hash = "items";

    const qrImageUrl =
        "https://api.qrserver.com/v1/create-qr-code/?" +
        new URLSearchParams({
            size: "250x250",
            data: reportUrl.toString(),
        }).toString();

    async function copyReportLink() {
        try {
            await navigator.clipboard.writeText(reportUrl.toString());
            setMessage("Report link copied successfully.");
        } catch {
            setMessage("Unable to copy automatically. Copy the link below.");
        }
    }

    return (
        <div className="report-qr">
            <button
                type="button"
                className="button button-outline"
                onClick={() => {
                    setExpanded((previous) => !previous);
                    setMessage("");
                }}
                aria-expanded={expanded}
            >
                {expanded ? "Hide QR Code" : "Show QR Code"}
            </button>

            {expanded && (
                <div className="report-qr-content">
                    <h4>Scan to view this report</h4>

                    <img
                        src={qrImageUrl}
                        alt={`QR code for ${item.title || "item report"}`}
                        width="250"
                        height="250"
                        loading="lazy"
                        onError={() =>
                            setMessage(
                                "QR image could not load. Check your internet connection."
                            )
                        }
                    />

                    <p>
                        {item.title} — {item._type.toUpperCase()} REPORT
                    </p>

                    <a
                        href={qrImageUrl}
                        target="_blank"
                        rel="noreferrer"
                    >
                        Open QR image
                    </a>

                    <button
                        type="button"
                        className="button button-outline"
                        onClick={copyReportLink}
                    >
                        Copy report link
                    </button>

                    <a
                        className="button button-primary"
                        href={qrImageUrl}
                        download={`hero-find-${item._type}-${item.id}-qr.png`}
                        target="_blank"
                        rel="noreferrer"
                    >
                        Download QR image
                    </a>

                    <p>{message}</p>

                    <small>
                        Scanning the QR code opens this report on the current
                        website. The report must be publicly accessible to viewers.
                    </small>
                </div>
            )}
        </div>
    );
}