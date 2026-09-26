#!/usr/bin/env python3
import os
import sys
import time
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from googleapiclient.errors import HttpError

# OAuth Scopes
SCOPES = [
    "https://www.googleapis.com/auth/youtube.upload",
    "https://www.googleapis.com/auth/youtube"
]

OLD_EN_ID = "UKZu5ZsdORg"
OLD_ID_ID = "4CvpJkjaz-k"

REPO_FILES = [
    "README.md",
    "demo/README.md",
    "docs/PLAY_STORE_LISTING.md"
]

def get_authenticated_service():
    creds = None
    if os.path.exists("token.json"):
        try:
            creds = Credentials.from_authorized_user_file("token.json", SCOPES)
        except Exception:
            creds = None

    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            print("Refreshing expired access token...")
            creds.refresh(Request())
        else:
            if not os.path.exists("client_secrets.json"):
                print("Error: client_secrets.json not found in project root.")
                sys.exit(1)
            print("Opening browser for one-time YouTube authorization...")
            flow = InstalledAppFlow.from_client_secrets_file("client_secrets.json", SCOPES)
            creds = flow.run_local_server(port=0)

        with open("token.json", "w") as token_file:
            token_file.write(creds.to_json())
        print("Authorization token cached successfully in token.json")

    return build("youtube", "v3", credentials=creds)

def upload_video(youtube, file_path, title, description, tags, privacy_status="public"):
    if not os.path.exists(file_path):
        print(f"Error: Video file {file_path} not found.")
        sys.exit(1)

    print(f"\nUploading '{file_path}' ({os.path.getsize(file_path) / 1024 / 1024:.2f} MB)...")
    body = {
        "snippet": {
            "title": title,
            "description": description,
            "tags": tags,
            "categoryId": "28"  # Science & Technology
        },
        "status": {
            "privacyStatus": privacy_status,
            "selfDeclaredMadeForKids": False
        }
    }

    media = MediaFileUpload(file_path, chunksize=1024*1024*2, resumable=True, mimetype="video/mp4")
    request = youtube.videos().insert(part="snippet,status", body=body, media_body=media)

    response = None
    while response is None:
        status, response = request.next_chunk()
        if status:
            print(f"  Upload progress: {int(status.progress() * 100)}%")

    video_id = response.get("id")
    print(f"✅ Upload complete! Video ID: {video_id}")
    print(f"   URL: https://www.youtube.com/watch?v=k{video_id}")
    return video_id

def deprecate_old_video(youtube, old_video_id, new_video_url):
    try:
        req = youtube.videos().list(part="snippet,status", id=old_video_id)
        res = req.execute()
        items = res.get("items", [])
        if not items:
            return

        item = items[0]
        snippet = item["snippet"]
        status = item["status"]

        new_title = f"[Outdated] {snippet['title']}" if not snippet['title'].startswith("[Outdated]") else snippet['title']
        new_desc = f"⚠️ THIS DEMO IS OUTDATED.\nWatch the latest version here: {new_video_url}\n\n" + snippet["description"]

        youtube.videos().update(
            part="snippet,status",
            body={
                "id": old_video_id,
                "snippet": {
                    "title": new_title[:100],
                    "description": new_desc[:5000],
                    "categoryId": snippet.get("categoryId", "28")
                },
                "status": {
                    "privacyStatus": "unlisted"
                }
            }
        ).execute()
        print(f"Updated old video {old_video_id} to [Outdated] and Unlisted.")
    except Exception as e:
        print(f"Note: Could not deprecate old video {old_video_id} ({e}). Skipping.")

def update_repo_links(old_id, new_id):
    if not old_id or not new_id or old_id == new_id:
        return

    for path in REPO_FILES:
        if not os.path.exists(path):
            continue
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()

        if old_id in content:
            updated = content.replace(old_id, new_id)
            with open(path, "w", encoding="utf-8") as f:
                f.write(updated)
            print(f"Updated {path}: replaced {old_id} -> {new_id}")

def main():
    youtube = get_authenticated_service()

    # 1. English Demo
    en_desc = (
        "Sotto is a dignified, low-stimulation assistive communication (AAC) app designed for "
        "autistic teens, adults, speech-impaired individuals, and non-speaking users who need "
        "clear, dignified text-to-speech support.\n\n"
        "Key Features Demonstrated:\n"
        "• Modern Two-Tier Quick-Speak Bar with instant speech & fullscreen expand\n"
        "• Emergency Bystander Hero Card with bilingual speech controls\n"
        "• Two-Way Caregiver Receptive Mode with 180° face-to-face screen flip & rapid responses (Yes, No, Repeat, Wait)\n"
        "• Dual-Language Speech & On-Device ML Kit Auto-Translation\n"
        "• Configurable Voice Settings & Two-Tone Attention Chime\n\n"
        "GitHub: https://github.com/triton1502oc/Sotto\n"
        "Google Play: https://play.google.com/apps/testing/com.amh.sotto"
    )
    en_tags = ["AAC", "Autism", "Assistive Communication", "Text to Speech", "TTS", "Speech Impairment", "Nonverbal", "Accessibility", "Android"]
    new_en_id = upload_video(
        youtube,
        file_path="demo/sotto_demo_en.mp4",
        title="Sotto - Dignified AAC Assistive Communication App Demo",
        description=en_desc,
        tags=en_tags,
        privacy_status="public"
    )

    # 2. Indonesian Demo
    id_desc = (
        "Sotto adalah aplikasi komunikasi augmentatif dan alternatif (AAC) yang tenang dan "
        "minim stimulasi untuk pengguna autis, gangguan bicara, afasia, dan non-verbal yang membutuhkan "
        "dukungan text-to-speech yang bermartabat.\n\n"
        "Fitur Utama yang Ditampilkan:\n"
        "• Quick-Speak Bar dua tingkat dengan layar penuh & bicara instan\n"
        "• Kartu Darurat Layar Penuh dengan tombol bicara dwibahasa\n"
        "• Mode Reseptif Dua Arah Caregiver dengan putar layar 180° & respon cepat (Ya, Tidak, Ulangi, Tunggu)\n"
        "• Pengalihan Bahasa Instan (TopBar Switcher) & Terjemahan On-Device\n"
        "• Pengaturan Suara & Nada Perhatian Dua Nada (Attention Chime)\n\n"
        "GitHub: https://github.com/triton1502oc/Sotto\n"
        "Google Play: https://play.google.com/apps/testing/com.amh.sotto"
    )
    id_tags = ["AAC", "Autisme", "Aplikasi Komunikasi", "Text to Speech", "TTS", "Gangguan Bicara", "Aksesibilitas", "Android", "Bahasa Indonesia"]
    new_id_id = upload_video(
        youtube,
        file_path="demo/sotto_demo_id.mp4",
        title="Sotto - Demo Aplikasi Komunikasi AAC & Mode Pendamping",
        description=id_desc,
        tags=id_tags,
        privacy_status="public"
    )

    # 3. Update links in repo
    print("\nUpdating repository documentation links...")
    update_repo_links(OLD_EN_ID, new_en_id)
    update_repo_links(OLD_ID_ID, new_id_id)

    # 4. Attempt to deprecate old videos
    print("\nDeprecating previous videos if permissions allow...")
    deprecate_old_video(youtube, OLD_EN_ID, f"https://www.youtube.com/watch?v={new_en_id}")
    deprecate_old_video(youtube, OLD_ID_ID, f"https://www.youtube.com/watch?v={new_id_id}")

    print("\n🎉 All tasks completed successfully!")
    print(f"English Demo:    https://www.youtube.com/watch?v={new_en_id}")
    print(f"Indonesian Demo: https://www.youtube.com/watch?v={new_id_id}")

if __name__ == "__main__":
    main()
