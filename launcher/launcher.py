"""
PvP Client Launcher (Python 3.9+, no extra packages).

What it does:
  1. Installs Fabric for Minecraft 1.21.1 as a profile in the OFFICIAL Minecraft Launcher
  2. Creates a separate game folder (~/.pvpclient) with Fabric API + the PvP Client mod
  3. Lets you add texture pack .zip files to that folder
Then you just press Play in the official launcher on the "PvP Client" profile.
(No account handling here, so your login stays safely in the official launcher.)

Before first use: run Minecraft 1.21.1 once from the official launcher.
"""
import json, os, platform, shutil, sys, urllib.parse, urllib.request
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

MC_VERSION = "1.21.1"
PROFILE_NAME = "PvP Client"
GAME_DIR = os.path.join(os.path.expanduser("~"), ".pvpclient")


def mc_dir():
    s = platform.system()
    if s == "Windows":
        return os.path.join(os.environ.get("APPDATA", ""), ".minecraft")
    if s == "Darwin":
        return os.path.expanduser("~/Library/Application Support/minecraft")
    return os.path.expanduser("~/.minecraft")


def get_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "pvpclient-launcher/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def download(url, dest):
    req = urllib.request.Request(url, headers={"User-Agent": "pvpclient-launcher/1.0"})
    with urllib.request.urlopen(req, timeout=60) as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f)


def install_fabric(log):
    base = mc_dir()
    if not os.path.isdir(os.path.join(base, "versions", MC_VERSION)):
        raise RuntimeError(f"Run Minecraft {MC_VERSION} once in the official launcher first.")
    loaders = get_json(f"https://meta.fabricmc.net/v2/versions/loader/{MC_VERSION}")
    loader = next(l["loader"]["version"] for l in loaders if l["loader"].get("stable", True))
    vid = f"fabric-loader-{loader}-{MC_VERSION}"
    log(f"Fetching Fabric profile {vid}")
    profile = get_json(f"https://meta.fabricmc.net/v2/versions/loader/{MC_VERSION}/{loader}/profile/json")
    vdir = os.path.join(base, "versions", vid)
    os.makedirs(vdir, exist_ok=True)
    with open(os.path.join(vdir, vid + ".json"), "w") as f:
        json.dump(profile, f)

    lp_path = os.path.join(base, "launcher_profiles.json")
    lp = json.load(open(lp_path)) if os.path.exists(lp_path) else {"profiles": {}}
    lp.setdefault("profiles", {})["pvpclient"] = {
        "name": PROFILE_NAME, "type": "custom", "lastVersionId": vid,
        "gameDir": GAME_DIR, "icon": "Furnace",
    }
    with open(lp_path, "w") as f:
        json.dump(lp, f, indent=2)
    log(f"Profile '{PROFILE_NAME}' added to the Minecraft Launcher")


def install_fabric_api(log):
    q = urllib.parse.quote
    url = ("https://api.modrinth.com/v2/project/fabric-api/version"
           f"?game_versions={q(json.dumps([MC_VERSION]))}&loaders={q(json.dumps(['fabric']))}")
    versions = get_json(url)
    if not versions:
        raise RuntimeError("No Fabric API build found for this version")
    f = versions[0]["files"][0]
    mods = os.path.join(GAME_DIR, "mods")
    os.makedirs(mods, exist_ok=True)
    log(f"Downloading {f['filename']}")
    download(f["url"], os.path.join(mods, f["filename"]))


def install_mod_jar(path, log):
    mods = os.path.join(GAME_DIR, "mods")
    os.makedirs(mods, exist_ok=True)
    shutil.copy(path, mods)
    log(f"Installed {os.path.basename(path)}")


def add_packs(paths, log):
    rp = os.path.join(GAME_DIR, "resourcepacks")
    os.makedirs(rp, exist_ok=True)
    for p in paths:
        shutil.copy(p, rp)
        log(f"Added pack {os.path.basename(p)}")


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("PvP Client Launcher")
        self.geometry("460x360")
        ttk.Label(self, text=f"PvP Client - Minecraft {MC_VERSION}", font=("Segoe UI", 14, "bold")).pack(pady=10)
        for text, cmd in [
            ("1. Install Fabric + Fabric API", self.on_install),
            ("2. Install PvP Client mod (.jar)", self.on_mod),
            ("3. Add texture packs (.zip)", self.on_packs),
            ("Open game folder", self.on_open),
        ]:
            ttk.Button(self, text=text, command=cmd).pack(fill="x", padx=30, pady=4)
        self.out = tk.Text(self, height=8, state="disabled")
        self.out.pack(fill="both", expand=True, padx=10, pady=10)

    def log(self, msg):
        self.out.config(state="normal"); self.out.insert("end", msg + "\n")
        self.out.config(state="disabled"); self.out.see("end"); self.update()

    def run(self, fn, *a):
        try:
            fn(*a, self.log)
        except Exception as e:
            messagebox.showerror("Error", str(e)); self.log(f"Error: {e}")

    def on_install(self):
        self.run(install_fabric); self.run(install_fabric_api)
        self.log("Done. Open the Minecraft Launcher and pick the 'PvP Client' profile.")

    def on_mod(self):
        p = filedialog.askopenfilename(title="Select pvpclient-*.jar", filetypes=[("Jar", "*.jar")])
        if p: self.run(install_mod_jar, p)

    def on_packs(self):
        ps = filedialog.askopenfilenames(title="Select texture packs", filetypes=[("Zip", "*.zip")])
        if ps: self.run(add_packs, ps)

    def on_open(self):
        os.makedirs(GAME_DIR, exist_ok=True)
        if platform.system() == "Windows": os.startfile(GAME_DIR)
        elif platform.system() == "Darwin": os.system(f'open "{GAME_DIR}"')
        else: os.system(f'xdg-open "{GAME_DIR}"')


if __name__ == "__main__":
    App().mainloop()
