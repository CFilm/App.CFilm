#!/usr/bin/env python3
"""
Streamtape Remote Upload & Auto-Renewer for CFilm
Automates server-side remote uploading of video links into existing user-created folders:
- Movies: Located in existing 'peliculas' folder in root.
- Series: Located in existing 'series/[Title]/s[Season]' subfolders (e.g., 'series/Breaking Bad/s1').
- Anime: Located in existing 'anime/[Title]/s[Season]' subfolders (e.g., 'anime/Naruto/s1').

100% Automated Inactive Videos Renewer:
- Scrapes Streamtape's 'Inactive Videos' page directly (no manual copying needed).
- Automatically parses filename, detects Category (Anime/Series/Movie), Title, and Season.
- Locates the existing folder structure created by user and re-uploads each video.
- Displays detailed URL verification logs and summary table.
- Leaves old expiring files untouched (Streamtape will auto-delete them).
"""

import sys
import os
import re
import json
import time
import ssl
import argparse
import urllib.request
import urllib.parse
from typing import Dict, Any, Optional, List, Tuple

BASE_API_URL = "https://api.streamtape.com"

# SSL Context for HTTPS requests
ssl_context = ssl._create_unverified_context()

def load_credentials_from_secrets(secrets_file: str) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    """Tries to read STREAMTAPE_LOGIN, STREAMTAPE_KEY, and STREAMTAPE_COOKIE from secrets.properties."""
    login, key, cookie = None, None, None
    if os.path.exists(secrets_file):
        try:
            with open(secrets_file, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line.startswith("STREAMTAPE_LOGIN="):
                        login = line.split("=", 1)[1].strip()
                    elif line.startswith("STREAMTAPE_KEY="):
                        key = line.split("=", 1)[1].strip()
                    elif line.startswith("STREAMTAPE_COOKIE="):
                        cookie = line.split("=", 1)[1].strip()
        except Exception as e:
            print(f"[!] Warning reading secrets file: {e}")
    return login, key, cookie

def send_dev_notification(subject: str, message: str, topic: str = "cfilm_alertas_streamtape"):
    """Sends a real-time push notification to developer's phone/devices via ntfy.sh (100% free, instant)."""
    try:
        url = f"https://ntfy.sh/{topic}"
        req = urllib.request.Request(
            url,
            data=message.encode("utf-8"),
            headers={
                "Title": subject,
                "Tags": "warning,tv,film",
                "Priority": "high"
            }
        )
        with urllib.request.urlopen(req, context=ssl_context, timeout=10) as resp:
            print(f"[✓] Real-time push notification sent to '{topic}'")
    except Exception as e:
        print(f"[!] Warning sending notification: {e}")

def make_api_request(endpoint: str, params: Dict[str, Any]) -> Dict[str, Any]:
    """Sends a GET request to Streamtape API and returns parsed JSON result."""
    url_params = urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})
    full_url = f"{BASE_API_URL}/{endpoint}?{url_params}"

    req = urllib.request.Request(
        full_url,
        headers={"User-Agent": "CFilm-Streamtape-Uploader/8.0"}
    )

    try:
        with urllib.request.urlopen(req, context=ssl_context, timeout=30) as response:
            data = response.read().decode("utf-8")
            return json.loads(data)
    except Exception as e:
        return {"status": 500, "msg": f"HTTP Error: {str(e)}", "result": None}

def parse_media_info_from_filename(filename_or_url: str) -> Tuple[str, Optional[int], Optional[int], str]:
    """
    Parses filename or URL to extract (title, season, episode, category).
    category is one of: 'movie', 'series', 'anime'
    Example: 'naruto_s01e02.mp4' -> ('Naruto', 1, 2, 'anime')
    Example: 'breaking_bad_s01e02.mp4' -> ('Breaking Bad', 1, 2, 'series')
    Example: 'avatar_the_way_of_water_2022.mp4' -> ('Avatar The Way Of Water', None, None, 'movie')
    """
    unquoted = urllib.parse.unquote(filename_or_url)
    clean_name = unquoted.split("?")[0].split("#")[0].split("/")[-1]
    clean_name = re.sub(r"\.(mp4|mkv|avi|mov|wmv|flv|webm|m3u8)$", "", clean_name, flags=re.IGNORECASE)

    lower_full = unquoted.lower()
    is_anime_keyword = "anime" in lower_full or "animé" in lower_full or "naruto" in lower_full or "one piece" in lower_full or "dragon ball" in lower_full or "shingeki" in lower_full or "boruto" in lower_full or "bleach" in lower_full or "jujutsu" in lower_full or "demon slayer" in lower_full or "kimetsu" in lower_full

    name_clean = re.sub(r"[._\-/]", " ", clean_name)

    # Match S01E02 / S1E2 / S01.E02
    m1 = re.search(r"(?i)\bs(\d{1,2})[\s._-]*e(\d{1,3})\b", name_clean)
    if m1:
        season = int(m1.group(1))
        episode = int(m1.group(2))
        title_part = name_clean[:m1.start()].strip()
        title = title_part.title() if title_part else "Series"
        category = "anime" if is_anime_keyword else "series"
        return title, season, episode, category

    # Match 1x02 / 01x02
    m2 = re.search(r"(?i)\b(\d{1,2})x(\d{1,3})\b", name_clean)
    if m2:
        season = int(m2.group(1))
        episode = int(m2.group(2))
        title_part = name_clean[:m2.start()].strip()
        title = title_part.title() if title_part else "Series"
        category = "anime" if is_anime_keyword else "series"
        return title, season, episode, category

    # Match Season 1 Episode 2 / Temp 1 Cap 2
    m3 = re.search(r"(?i)\b(?:season|temp|temporada)\s*(\d{1,2})[\s._-]*(?:ep|episodio|cap|capitulo|e)?\s*(\d{1,3})\b", name_clean)
    if m3:
        season = int(m3.group(1))
        episode = int(m3.group(2))
        title_part = name_clean[:m3.start()].strip()
        title = title_part.title() if title_part else "Series"
        category = "anime" if is_anime_keyword else "series"
        return title, season, episode, category

    # Match standalone Episode/Cap e.g. "cap 4", "ep 02"
    m4 = re.search(r"(?i)\b(?:ep|episodio|cap|capitulo|e)\s*(\d{1,3})\b", name_clean)
    if m4:
        season = 1
        episode = int(m4.group(1))
        title_part = name_clean[:m4.start()].strip()
        title = title_part.title() if title_part else "Series"
        category = "anime" if is_anime_keyword else "series"
        return title, season, episode, category

    # Default: Movie
    title = re.sub(r"\b(19|20)\d{2}\b", "", name_clean).strip().title()
    return title or "Pelicula", None, None, "movie"

class StreamtapeUploader:
    def __init__(self, login: str, key: str, cookie: Optional[str] = None):
        self.login = login
        self.key = key
        self.cookie = cookie
        self.folder_cache: Dict[Tuple[Optional[str], str], str] = {}

    def list_folders(self, parent_folder_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """Lists all folders in Streamtape account under parent_folder_id (or root)."""
        res = make_api_request("file/listfolder", {
            "login": self.login,
            "key": self.key,
            "folder": parent_folder_id
        })
        if res.get("status") == 200 and res.get("result"):
            return res["result"].get("folders", [])
        return []

    def get_existing_folder(self, folder_name: str, parent_folder_id: Optional[str] = None) -> Optional[str]:
        """
        Locates an existing folder matching folder_name under parent_folder_id.
        Does NOT create any new folder if not found. Returns folder_id if found, or parent_folder_id.
        """
        cache_key = (parent_folder_id, folder_name.strip().lower())
        if cache_key in self.folder_cache:
            return self.folder_cache[cache_key]

        existing_folders = self.list_folders(parent_folder_id)
        for folder in existing_folders:
            if folder.get("name", "").strip().lower() == folder_name.strip().lower():
                folder_id = folder["id"]
                self.folder_cache[cache_key] = folder_id
                print(f"[✓] Located existing folder '{folder.get('name')}' -> ID: {folder_id}")
                return folder_id

        # Folder not found -> DO NOT create, log info
        parent_desc = f"under parent ID '{parent_folder_id}'" if parent_folder_id else "in root"
        print(f"[!] Folder '{folder_name}' not found {parent_desc}. (Using parent location).")
        return parent_folder_id

    def get_target_folder_id_for_media(self, category: str, title: str, season: Optional[int] = None) -> Optional[str]:
        """
        Locates existing Streamtape folder structure:
        - Movies ('movie') -> 'peliculas' (root folder)
        - Series ('series') -> 'series' / '[Title]' / 's[Season]' (e.g. 'series/Breaking Bad/s1')
        - Anime ('anime')   -> 'anime'  / '[Title]' / 's[Season]' (e.g. 'anime/Naruto/s1')
        """
        cat = category.strip().lower()

        if cat in ("movie", "peliculas", "película", "pelicula"):
            return self.get_existing_folder("peliculas", parent_folder_id=None)

        elif cat == "anime":
            anime_root_id = self.get_existing_folder("anime", parent_folder_id=None)
            if not anime_root_id:
                return None
            title_folder_id = self.get_existing_folder(title, parent_folder_id=anime_root_id)
            if not title_folder_id:
                return anime_root_id
            season_folder_name = f"s{season or 1}"
            return self.get_existing_folder(season_folder_name, parent_folder_id=title_folder_id)

        else:
            series_root_id = self.get_existing_folder("series", parent_folder_id=None)
            if not series_root_id:
                return None
            title_folder_id = self.get_existing_folder(title, parent_folder_id=series_root_id)
            if not title_folder_id:
                return series_root_id
            season_folder_name = f"s{season or 1}"
            return self.get_existing_folder(season_folder_name, parent_folder_id=title_folder_id)

    def add_remote_upload(self, video_url: str, folder_id: Optional[str] = None) -> Optional[Dict[str, Any]]:
        """Connects to Streamtape /remotedl/add endpoint to start server-side transfer."""
        print(f"[*] Submitting remote upload for: {video_url}")
        res = make_api_request("remotedl/add", {
            "login": self.login,
            "key": self.key,
            "url": video_url,
            "folder": folder_id
        })

        if res.get("status") == 200 and res.get("result"):
            result = res["result"]
            upload_id = result.get("id")
            print(f"[✓] Remote upload added successfully! (ID: {upload_id})")
            return result
        else:
            print(f"[!] Remote upload failed: {res.get('msg')}")
            return None

    def check_remote_status(self) -> Dict[str, Any]:
        """Queries status of active/pending remote downloads."""
        res = make_api_request("remotedl/status", {"login": self.login, "key": self.key})
        if res.get("status") != 200 or not res.get("result"):
            res = make_api_request("remotedl/list", {"login": self.login, "key": self.key})
        return res

    def fetch_inactive_links_from_streamtape_panel(self) -> List[str]:
        """
        Scrapes 'https://streamtape.com/accpanel?inactive' automatically
        using session cookie or login credentials to fetch all expiring video links.
        """
        print("[*] Connecting to Streamtape panel 'Inactive Videos'...")
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        }
        if self.cookie:
            headers["Cookie"] = self.cookie

        url = "https://streamtape.com/accpanel?inactive"
        req = urllib.request.Request(url, headers=headers)

        try:
            with urllib.request.urlopen(req, context=ssl_context, timeout=30) as resp:
                html = resp.read().decode("utf-8", errors="ignore")
                found_links = re.findall(r"https://streamtape\.com/v/[a-zA-Z0-9]+/[^\"'\s<>]+", html)
                if not found_links:
                    found_links = [f"https://streamtape.com{path}" for path in re.findall(r"/v/[a-zA-Z0-9]+/[^\"'\s<>]+", html)]

                distinct_links = list(dict.fromkeys(found_links))
                print(f"[✓] Scraped {len(distinct_links)} inactive link(s) from Streamtape panel.")
                if distinct_links:
                    send_dev_notification(
                        subject="🚨 Alerta CFilm: Videos Inactivos Detectados",
                        message=f"Se detectaron {len(distinct_links)} video(s) inactivos en Streamtape. Iniciando re-subida..."
                    )
                return distinct_links
        except Exception as e:
            print(f"[!] Warning: Could not scrape panel directly: {e}")
            print("    Make sure STREAMTAPE_COOKIE=... is saved in secrets.properties.")
            return []

    def process_inactive_video_links(self, urls: List[str]):
        """
        Takes a list of expiring links copied or scraped from Streamtape 'Inactive Videos'.
        Automatically parses media info for each link, locates the existing target folder
        (peliculas, series/[Title]/s[Season], or anime/[Title]/s[Season]), and triggers remote upload.
        Displays detailed verification logs for each URL.
        """
        if not urls:
            print("[!] No inactive video links found to process.")
            return

        print(f"\n=========================================================================")
        print(f"[*] LIST OF INACTIVE LINKS DETECTED ({len(urls)} file(s)):")
        print(f"=========================================================================")
        for i, u in enumerate(urls, 1):
            print(f"  [{i}] {u}")
        print(f"=========================================================================\n")

        results_list = []
        success_count = 0

        for idx, video_url in enumerate(urls, 1):
            print(f"--- [{idx}/{len(urls)}] Processing Expiring Link ---")
            print(f" 🔗 Original Expiring Link : {video_url}")

            title, season, episode, category = parse_media_info_from_filename(video_url)
            ep_str = f"e{episode}" if episode else ""
            print(f" 📂 Detected Category      : '{category.upper()}'")
            print(f" 🎬 Detected Title         : '{title}'")
            print(f" 📌 Season / Episode       : 's{season or 1}{ep_str}'")

            target_folder_id = self.get_target_folder_id_for_media(category, title, season)
            result = self.add_remote_upload(video_url, folder_id=target_folder_id)

            if result:
                upload_id = result.get("id")
                success_count += 1
                results_list.append({
                    "url": video_url,
                    "title": title,
                    "category": category,
                    "season": season or 1,
                    "upload_id": upload_id,
                    "folder_id": target_folder_id
                })

        print(f"\n=========================================================================")
        print(f"[✓] RENEWAL COMPLETED SUMMARY ({success_count}/{len(urls)} link(s) renewed):")
        print(f"=========================================================================")
        for item in results_list:
            print(f"  • Enlace: {item['url']}")
            print(f"    └─> Destino   : {item['category'].upper()} / {item['title']} / s{item['season']} (Folder ID: {item['folder_id']})")
            print(f"    └─> Remote ID : {item['upload_id']}")
        print(f"=========================================================================\n")

        send_dev_notification(
            subject="🎬 CFilm: Renovación Automática de Videos Completa",
            message=f"Se procesaron e iniciaron las re-subidas remotas de {success_count} de {len(urls)} videos inactivos en Streamtape."
        )

def main():
    parser = argparse.ArgumentParser(description="Streamtape Inactive Videos Auto-Renewer & Structurer for CFilm")
    parser.add_argument("--fetch-inactive", action="store_true", help="Scrape Streamtape panel 'Inactive Videos' and re-upload all expiring files automatically")
    parser.add_argument("--url", type=str, help="Direct video URL or Streamtape URL to upload/renew")
    parser.add_argument("--file", type=str, help="Path to text file containing list of inactive video URLs")
    parser.add_argument("--inactive-file", type=str, help="Alias for --file")

    # Manual Category & Title Overrides
    parser.add_argument("--movie", action="store_true", help="Upload as movie into root 'peliculas' folder")
    parser.add_argument("--series", type=str, help="Series title (uploads to 'series/[Title]/s[Season]')")
    parser.add_argument("--anime", type=str, help="Anime title (uploads to 'anime/[Title]/s[Season]')")
    parser.add_argument("--season", type=int, help="Season number (e.g. 1 for 's1')")
    parser.add_argument("--type", type=str, choices=["movie", "series", "anime"], help="Explicit category type")

    parser.add_argument("--list-folders", action="store_true", help="List all root folders in Streamtape")
    parser.add_argument("--status", action="store_true", help="Check status of active remote uploads")
    parser.add_argument("--login", type=str, help="Streamtape API Login ID")
    parser.add_argument("--key", type=str, help="Streamtape API Key")
    parser.add_argument("--cookie", type=str, help="Streamtape Session Cookie for web panel scraping")

    args = parser.parse_args()

    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    secrets_path = os.path.join(project_root, "secrets.properties")

    sec_login, sec_key, sec_cookie = load_credentials_from_secrets(secrets_path)
    login = args.login or sec_login
    key = args.key or sec_key
    cookie = args.cookie or sec_cookie

    if not login or not key:
        print("[!] Error: STREAMTAPE_LOGIN and STREAMTAPE_KEY are required in secrets.properties.")
        sys.exit(1)

    uploader = StreamtapeUploader(login, key, cookie=cookie)

    if args.list_folders:
        print("\n--- Listing Streamtape Root Folders ---")
        folders = uploader.list_folders()
        for f in folders:
            print(f"📁 Folder ID: {f.get('id')}  |  Name: {f.get('name')}")
        return

    if args.status:
        print("\n--- Remote Upload Status ---")
        status_res = uploader.check_remote_status()
        print(json.dumps(status_res, indent=2, ensure_ascii=False))
        return

    file_path = args.file or args.inactive_file
    if file_path:
        if not os.path.exists(file_path):
            print(f"[!] File not found: {file_path}")
            sys.exit(1)
        with open(file_path, "r", encoding="utf-8") as f:
            urls = [line.strip() for line in f if line.strip() and not line.strip().startswith("#")]

        uploader.process_inactive_video_links(urls)
        return

    if args.fetch_inactive:
        urls = uploader.fetch_inactive_links_from_streamtape_panel()
        if urls:
            uploader.process_inactive_video_links(urls)
        else:
            print("[!] No inactive links fetched automatically. You can also pass a file via --file inactivos.txt")
        return

    if args.url:
        if args.movie or args.type == "movie":
            category = "movie"
            title = "Pelicula"
            season = None
        elif args.anime or args.type == "anime":
            category = "anime"
            title = args.anime or "Anime"
            season = args.season or 1
        elif args.series or args.type == "series":
            category = "series"
            title = args.series or "Series"
            season = args.season or 1
        else:
            title, season, episode, category = parse_media_info_from_filename(args.url)
            print(f"[*] Auto-detected: Category='{category}', Title='{title}', Season={season or 'N/A'}, Episode={episode or 'N/A'}")

        target_folder_id = uploader.get_target_folder_id_for_media(category, title, season)
        uploader.add_remote_upload(args.url, folder_id=target_folder_id)
        return

    parser.print_help()

if __name__ == "__main__":
    main()
