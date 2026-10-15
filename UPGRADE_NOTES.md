# Lost & Found Matcher — upgrade notes

This source package includes the React/Vite frontend and Spring Boot backend.

## Changes included

- Public GET access for community item lists and categories; report creation and account-owned endpoints still require JWT authentication.
- Frontend request timeout and visible retry/error states so requests do not spin forever.
- Frontend signup/signin connected to the existing `/auth/register` and `/auth/login` API routes.
- Report payloads now use the backend's real `categoryId`, `lostDate`, and `foundDate` fields.
- Default categories are inserted at startup if they do not exist.
- JPEG/PNG/WebP upload endpoint (`POST /api/images/upload`, multipart field `file`), saved image URL attached to lost/found reports, and image previews on case cards.
- Optional browser geolocation; exact coordinates are used on the server for distance-based matching, while API responses return rounded coordinates for approximate map display.
- Marvel-inspired red/black comic-book styling, designed as fan-inspired UI and not affiliated with Marvel.
- Fixed found-item listing to query `ACTIVE` reports, matching the status used when creating reports.
- CORS allows localhost and Vercel-hosted frontend deployments.

## Frontend deployment

Vercel project settings:

- Framework: Vite
- Build command: `npm run build`
- Output directory: `dist`
- Environment variable: `VITE_API_BASE_URL=https://lost-found-backend-equb.onrender.com`

## Backend deployment

Push the backend source changes to the repository Render builds from. Render will rebuild and restart the backend. Database schema changes are added by the existing Hibernate `ddl-auto=update` setting.

The backend now accepts Vercel origins matching `https://*.vercel.app` plus localhost development origins. If using a custom frontend domain, add it to `CorsConfig`.

### Important: uploaded-image persistence on Render

The image endpoint writes files to the directory set by `UPLOAD_DIR` (default: `uploads`). Render service filesystems are ephemeral unless a persistent disk is attached. To keep uploaded photos across deploys/restarts, attach a Render persistent disk mounted at `/var/data` and add the backend environment variable:

`UPLOAD_DIR=/var/data/uploads`

Without a persistent disk (or external object storage), uploaded files can disappear after a redeploy/restart. For a production launch, object storage such as S3/Cloudinary is also a suitable alternative.

## Feature boundaries / privacy

- Image uploads let people compare photos on the case cards; the current matcher does not run a computer-vision model to infer image similarity. Existing matching scores use title, description, category, and location, with coordinate distance when both reports have coordinates.
- Location is optional and browser permission is required. If users deny permission, they can type a landmark manually.
- Public map coordinates are rounded to reduce exposing exact locations. Precise coordinates remain stored for server-side matching; consider a more privacy-focused retention policy before production.
- Never commit `.env`, `backend.env`, JWT secrets, database passwords, or uploaded files. Rotate any credential that was ever committed or shared.
