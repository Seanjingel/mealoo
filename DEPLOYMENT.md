# Mealoo Deployment Guide

## Prerequisites

- Java 17+
- Maven (or use included `./mvnw`)
- Meta Developer Account — [developers.facebook.com](https://developers.facebook.com)
- WhatsApp Business Account linked in Meta Developer Console

---

## Meta Developer Setup (Required for both environments)

### Step 1 — Create a Meta App

1. Go to [developers.facebook.com](https://developers.facebook.com) → **My Apps** → **Create App**
2. Choose **Business** type
3. Add **WhatsApp** product to the app

### Step 2 — Get Credentials

From **WhatsApp → API Setup** in your app dashboard, copy:

| Value | Where to find |
|---|---|
| `Phone Number ID` | Listed under "From" phone number |
| `Access Token` | Temporary token shown on API Setup page (use System User token for production) |
| `WhatsApp Business Account ID` | Shown at top of API Setup page |

### Step 3 — Update application.properties

```properties
whatsapp.cloud.api.url=https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages
whatsapp.cloud.access.token=YOUR_ACCESS_TOKEN
whatsapp.verify.token=mealoo_verify_token_2024
```

---

## Test Deployment (Local + ngrok)

### Step 1 — Install ngrok

```bash
# Windows (via Chocolatey)
choco install ngrok

# Or download directly from https://ngrok.com/download
```

Sign up at [ngrok.com](https://ngrok.com) and authenticate:
```bash
ngrok config add-authtoken YOUR_NGROK_TOKEN
```

### Step 2 — Start the Application

```bash
./mvnw spring-boot:run
```

App starts on `http://localhost:8080`.

### Step 3 — Start ngrok Tunnel

Open a second terminal:
```bash
ngrok http 8080
```

You will see output like:
```
Forwarding   https://abc123.ngrok-free.app -> http://localhost:8080
```

Copy the `https://` URL.

### Step 4 — Register Webhook with Meta

1. Go to your Meta App → **WhatsApp → Configuration**
2. Under **Webhook**, click **Edit**
3. Set:
   - **Callback URL**: `https://abc123.ngrok-free.app/webhook/whatsapp`
   - **Verify Token**: `mealoo_verify_token_2024`
4. Click **Verify and Save**
5. Under **Webhook Fields**, subscribe to **messages**

### Step 5 — Add Test Phone Number

1. Go to **WhatsApp → API Setup**
2. Under **To**, add your personal WhatsApp number
3. Send a test message from the sandbox

### Step 6 — Test the Flow

Send **Hi** to your WhatsApp Business number — you should receive an interactive list of restaurants.

> **Note:** ngrok free tier generates a new URL on every restart. Re-register the webhook URL in Meta each time you restart ngrok.

---

## Production Deployment

### Option A — Railway (Recommended for quick deploy)

1. **Create account** at [railway.app](https://railway.app)

2. **Push code to GitHub** (if not already):
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USERNAME/mealoo.git
   git push -u origin main
   ```

3. **Deploy on Railway:**
   - New Project → Deploy from GitHub repo → Select `mealoo`
   - Railway auto-detects Spring Boot and builds with Maven

4. **Set environment variables** in Railway dashboard → Variables:
   ```
   WHATSAPP_CLOUD_API_URL=https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages
   WHATSAPP_CLOUD_ACCESS_TOKEN=YOUR_SYSTEM_USER_ACCESS_TOKEN
   WHATSAPP_VERIFY_TOKEN=mealoo_verify_token_2024
   SERVER_PORT=8080
   ```

5. **Update application.properties** to read from env vars:
   ```properties
   whatsapp.cloud.api.url=${WHATSAPP_CLOUD_API_URL}
   whatsapp.cloud.access.token=${WHATSAPP_CLOUD_ACCESS_TOKEN}
   whatsapp.verify.token=${WHATSAPP_VERIFY_TOKEN}
   ```

6. Railway provides a permanent URL like `https://mealoo.up.railway.app`

7. **Register this URL as webhook** in Meta (same steps as test, but permanent URL)

---

### Option B — Render

1. Create account at [render.com](https://render.com)
2. New → **Web Service** → Connect GitHub repo
3. Build Command: `./mvnw clean package -DskipTests`
4. Start Command: `java -jar target/Mealoo-0.0.1-SNAPSHOT.jar`
5. Set environment variables same as Railway above
6. Render provides a permanent HTTPS URL

---

### Option C — AWS EC2

1. **Launch EC2 instance** (Ubuntu 22.04, t3.micro or higher)

2. **Install Java 17:**
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jdk -y
   ```

3. **Build JAR locally and copy to server:**
   ```bash
   ./mvnw clean package -DskipTests
   scp target/Mealoo-0.0.1-SNAPSHOT.jar ec2-user@YOUR_EC2_IP:/home/ec2-user/
   ```

4. **Create environment config on server:**
   ```bash
   nano /home/ec2-user/mealoo.env
   ```
   ```env
   WHATSAPP_CLOUD_API_URL=https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages
   WHATSAPP_CLOUD_ACCESS_TOKEN=YOUR_SYSTEM_USER_ACCESS_TOKEN
   WHATSAPP_VERIFY_TOKEN=mealoo_verify_token_2024
   ```

5. **Run as a systemd service:**
   ```bash
   sudo nano /etc/systemd/system/mealoo.service
   ```
   ```ini
   [Unit]
   Description=Mealoo WhatsApp Bot
   After=network.target

   [Service]
   User=ec2-user
   EnvironmentFile=/home/ec2-user/mealoo.env
   ExecStart=/usr/bin/java -jar /home/ec2-user/Mealoo-0.0.1-SNAPSHOT.jar
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```
   ```bash
   sudo systemctl enable mealoo
   sudo systemctl start mealoo
   sudo systemctl status mealoo
   ```

6. **Set up HTTPS with Nginx + Certbot:**
   ```bash
   sudo apt install nginx certbot python3-certbot-nginx -y
   sudo certbot --nginx -d yourdomain.com
   ```

   Nginx config (`/etc/nginx/sites-available/mealoo`):
   ```nginx
   server {
       listen 443 ssl;
       server_name yourdomain.com;

       location /webhook {
           proxy_pass http://localhost:8080/webhook;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

7. **Register `https://yourdomain.com/webhook/whatsapp`** as webhook in Meta

---

## Production Access Token (Permanent)

The temporary token from Meta API Setup expires in 24 hours. For production:

1. Go to **Business Settings** → **System Users** → **Add**
2. Create a system user with **Admin** role
3. Click **Generate New Token** → select your app → grant `whatsapp_business_messaging` permission
4. Copy the permanent token → use as `WHATSAPP_CLOUD_ACCESS_TOKEN`

---

## Webhook Verification Checklist

| Check | Expected |
|---|---|
| `GET /webhook/whatsapp?hub.mode=subscribe&hub.challenge=TEST&hub.verify_token=mealoo_verify_token_2024` | Returns `TEST` |
| App logs show incoming messages | `DEBUG com.mealoo` logs visible |
| Sending "Hi" on WhatsApp returns restaurant list | Interactive list message received |

---

## Troubleshooting

| Problem | Solution |
|---|---|
| Webhook verification fails (403) | Check `whatsapp.verify.token` matches Meta console exactly |
| Messages not received | Confirm webhook subscribed to **messages** field in Meta |
| Bot not replying | Check access token is valid and not expired |
| `401 Unauthorized` in logs | Regenerate access token in Meta Developer Console |
| ngrok URL changed | Re-register new ngrok URL in Meta webhook settings |
| App crashes on startup | Ensure all 3 `whatsapp.*` properties are set in application.properties |
