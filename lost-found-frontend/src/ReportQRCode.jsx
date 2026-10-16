
import { useState } from "react";
import "./ReportQRCode.css";

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
      setMessage("Copy failed. Select and copy the report link below.");
    }
  }

  return (
    <section className="report-qr">
      <button
        type="button"
        className="report-qr-toggle"
        onClick={() => {
          setExpanded((previous) => !previous);
          setMessage("");
        }}
        aria-expanded={expanded}
      >
        <span aria-hidden="true">▦</span>
        {expanded ? "Hide QR code" : "Generate report QR code"}
      </button>

      {expanded && (
        <div className="report-qr-panel">
          <h4>Share this report</h4>

          <div className="report-qr-image">
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
          </div>

          <p className="report-qr-title">
            {item.title || "Item report"}
          </p>

          <p className="report-qr-description">
            {String(item._type).toUpperCase()} REPORT ·
            Scan to open this report on TraceBack.
          </p>

          <div className="report-qr-actions">
            <button type="button" onClick={copyReportLink}>
              Copy report link
            </button>

            <a
              className="report-qr-action-link"
              href={qrImageUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              Open QR image
            </a>

            <a
              className="report-qr-action-link"
              href={qrImageUrl}
              download={`traceback-${item._type}-${item.id}-qr.png`}
              target="_blank"
              rel="noopener noreferrer"
            >
              Download QR image
            </a>
          </div>

          <label className="report-qr-link-label">
            Report URL
            <input
              type="url"
              readOnly
              value={reportUrl.toString()}
              onFocus={(event) => event.target.select()}
              aria-label="Report URL"
            />
          </label>

          {message && (
            <p className="report-qr-message" role="status">
              {message}
            </p>
          )}

          <p className="report-qr-note">
            Only share reports you are comfortable making accessible.
            The QR code opens this website; viewing the report may still
            require access supported by the application.
          </p>
        </div>
      )}
    </section>
  );
}
