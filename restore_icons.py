import os
from PIL import Image

def generate_icons(source_path):
    if not os.path.exists(source_path):
        print(f"Source file {source_path} not found.")
        return

    configs = [
        ("mipmap-mdpi", 48),
        ("mipmap-hdpi", 72),
        ("mipmap-xhdpi", 96),
        ("mipmap-xxhdpi", 144),
        ("mipmap-xxxhdpi", 192)
    ]

    base_res_dir = "app/src/main/res"

    with Image.open(source_path) as img:
        for folder, size in configs:
            folder_path = os.path.join(base_res_dir, folder)
            os.makedirs(folder_path, exist_ok=True)
            standard_icon = img.resize((size, size), Image.Resampling.LANCZOS)
            standard_icon.save(os.path.join(folder_path, "ic_launcher.png"))
            standard_icon.save(os.path.join(folder_path, "ic_launcher_round.png"))
            print(f"Generated icons for {folder} ({size}x{size})")

if __name__ == "__main__":
    generate_icons("app_icon.png")
