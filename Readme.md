# 🎬 Streamflow

**Streamflow** is a demo OTT platform built to showcase **Netflix-style video streaming architecture**, including **secure playback (DRM-style flow)**, **adaptive streaming**, **thumbnail previews**, and **efficient event-driven backend communication**.

This project is intended for **engineering demonstration and system design learning purposes**.

---

## 🚀 Project Purpose

The objective of Streamflow is to demonstrate how large-scale OTT platforms like Netflix deliver video content **securely, efficiently, and at scale**, using **industry-standard technologies**.

Streamflow focuses on:

- Secure content playback architecture
- Adaptive bitrate streaming
- Preview thumbnails on video seek
- Event-driven backend design
- CDN-first video delivery

---

## 🧠 What Streamflow Demonstrates

- Netflix-style adaptive streaming (HLS / MPEG-DASH)
- DRM-style license-based playback flow
- Encrypted video segment delivery
- Thumbnail previews during seek
- Efficient frontend → backend → CDN communication
- Event streaming for playback analytics

---

## 🏗️ High-Level Architecture

```
Client (Next.js Frontend)
│
├── Authentication (JWT)
│
├── Playback Request
│
├── DRM License Request
│
├── Signed CDN URL Access
│
▼
CDN (Encrypted Video Segments)
```

Backend services communicate asynchronously using **Kafka** for scalable event handling.

---

## 🧑‍💻 Tech Stack

### Frontend

- **Next.js** (React-based framework)
- TypeScript
- Tailwind CSS / shadcn-ui
- Shaka Player or Video.js (DRM-capable player)
- Client-side adaptive streaming logic

---

### Backend

- **Java Spring Boot**
- Spring Security (JWT-based authentication)
- REST APIs for content, auth, and playback
- DRM license simulation service
- Signed URL generation for CDN access

---

### Event Streaming & Messaging

- **Apache Kafka**
- Playback events:
  - Play
  - Pause
  - Seek
  - Buffering
  - Quality switch

- Kafka consumers for:
  - Analytics
  - Monitoring
  - Future recommendation logic

---

### Media & Storage

- **AWS S3**
  - Encrypted video segments
  - Thumbnail sprite sheets
  - Media metadata

- FFmpeg
  - Transcoding
  - Segmentation (HLS / DASH)
  - Thumbnail generation

---

### CDN & Delivery

- AWS CloudFront (or Cloudflare)
- Signed URL based access
- Cache-first video delivery
- Backend never streams video bytes

---

### Security & DRM (Demo-Level)

- AES-based video encryption
- License-based playback authorization
- Widevine-style DRM flow (demo simulation)
- Secure media pipeline via browser player

> ⚠️ Note
> Streamflow does **not** implement Netflix proprietary DRM.
> It demonstrates **industry-standard DRM architecture concepts**.

---

### Infrastructure & DevOps (Optional / Demo Scope)

- Docker (service containerization)
- AWS IAM (access control)
- Environment-based configuration
- CI-ready project structure

### Git hooks (Husky)

Pre-commit hooks run **lint** and **build** for the frontend before each commit. To install hooks, run from the repo root:

```bash
npm install
```

This runs the `prepare` script and configures Git to use `.husky/` for hooks. The pre-commit hook runs `streamflow-fe` lint and build; the commit is blocked if either fails.

---

## 🎥 Streaming Technology

- Streaming Protocol: **MPEG-DASH / HLS**
- Video Segmentation: 2–6 seconds per chunk
- Multiple Bitrates: Adaptive quality switching
- Player: Shaka Player / Video.js

---

## 🖼️ Preview Thumbnails (Netflix-Style Seek Preview)

- Pre-generated thumbnail sprite sheets
- Time-index based preview rendering
- No runtime frame extraction
- Instant preview during seek bar hover

---

## 📱 Frontend Pages

1. **Start Screen**
   - Platform introduction
   - Get Started / Sign In CTA

2. **Login Screen**
   - User authentication
   - Demo login support

3. **Home Screen**
   - Movies & Series listing
   - Featured content banner
   - Horizontal carousels

4. **Preview Page**
   - Content metadata
   - Thumbnail previews
   - Play CTA

5. **Video Player Page**
   - Fullscreen playback
   - Adaptive streaming
   - Loader & buffer visualization
   - Seek preview thumbnails

---

## 🔁 Navigation Flow

```
Start Screen
→ Login
→ Home (Movies & Series)
→ Preview Page
→ Video Player
```

---

## 📦 Content Processing Pipeline

```
Upload Video
↓
FFmpeg Transcoding (Multiple Bitrates)
↓
Segmenting (HLS / DASH)
↓
Encryption
↓
Thumbnail Sprite Generation
↓
Upload to AWS S3
↓
Delivery via CDN
```

---

## 📊 Event-Driven Playback Analytics (Kafka)

Example events:

- `VIDEO_PLAY_STARTED`
- `VIDEO_BUFFERING`
- `VIDEO_SEEK`
- `QUALITY_CHANGED`
- `VIDEO_COMPLETED`

These events enable:

- Playback analytics
- QoE monitoring
- Future recommendation demos

---

## ❌ What Streamflow Does NOT Do

- Does not replicate Netflix proprietary algorithms
- Does not bypass DRM systems
- Does not allow video downloads
- Not intended for production deployment

---

## 📄 Legal & Educational Disclaimer

> Streamflow is a **technical demonstration project** for learning and portfolio use only.
> Netflix, Widevine, AWS, and other referenced technologies are trademarks of their respective owners.
> No proprietary code or algorithms are used.

---

## 🌱 Future Enhancements

- Multi-profile user support
- Recommendation engine demo
- Subtitle & audio track switching
- License expiration handling
- Playback analytics dashboard
- DRM provider abstraction layer

---

## 👨‍💻 Author

Built by **Hitesh**
Software Engineer | Builder
