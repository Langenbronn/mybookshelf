"""
Gnome Library app icon: one description, several outputs.
  python make_icon.py <out_dir>
Writes:
  ic_launcher_background.xml, ic_launcher_foreground.xml   (Android adaptive icon, 108 x 108)
  ic_launcher.xml                                          (Android < 8 legacy round icon)
  icon_square.svg  (iOS 1024 icon, full square)   icon_round_preview.svg (preview with round mask)
"""
import sys, os

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

# ---------------------------------------------------------------- shape helpers
def circle(cx, cy, r):
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0 Z"

def rrect(x, y, w, h, r):
    return (f"M{x + r},{y} h{w - 2 * r} a{r},{r} 0 0,1 {r},{r} v{h - 2 * r} a{r},{r} 0 0,1 {-r},{r} "
            f"h{-(w - 2 * r)} a{r},{r} 0 0,1 {-r},{-r} v{-(h - 2 * r)} a{r},{r} 0 0,1 {r},{-r} Z")

def rect(x, y, w, h):
    return f"M{x},{y} h{w} v{h} h{-w} Z"

def star(x, y, s):
    return f"M{x},{y - s} Q{x},{y} {x + s},{y} Q{x},{y} {x},{y + s} Q{x},{y} {x - s},{y} Q{x},{y} {x},{y - s} Z"

def fill(d, color, alpha=None):
    return ("fill", d, color, alpha)

def stroke(d, color, width):
    return ("stroke", d, color, width)

# ---------------------------------------------------------------- colors (same as the app)
GREEN = "#2F6B4F"
GREEN_LIGHT = "#3E8462"
HAT = "#D84343"
HAT_BAND = "#A92F2F"
SKIN = "#F2C6A0"
NOSE = "#E8927C"
BEARD = "#F7F7F7"
BEARD_EDGE = "#CFCFCF"
EYES = "#2B2B2B"
TUNIC = "#3F6FB5"
SLEEVE = "#35609F"
BLUSH = "#F28B8B"
BOOK = "#7A4B2A"
PAGE = "#FFF8E7"
TEXT_LINE = "#CBBFA6"
WOOD = "#8A6A3B"
WOOD_DARK = "#6B5030"
AMBER = "#F6C744"
SPINES = ["#B8433A", "#E0A030", "#3F6FB5", "#8E5BA8", "#46A758", "#D9774A", "#F3E3C3"]

# ---------------------------------------------------------------- background (108 x 108)
background = [fill(rect(0, 0, 108, 108), GREEN), fill(circle(54, 46, 27), GREEN_LIGHT, 0.7)]
# A shelf of books behind the gnome, on both sides
x = 14.0
i = 0
while x < 96:
    w = 4.2 + (i * 7) % 3
    h = 13 + (i * 37) % 9
    if not (37 < x < 67):           # hidden behind the gnome anyway
        background.append(fill(rrect(x, 78 - h, w, h, 0.8), SPINES[(i * 3) % len(SPINES)]))
        background.append(fill(rect(x + 0.8, 78 - h + 2.5, w - 1.6, 0.8), "#FFF3D6", 0.45))
    x += w + 0.6
    i += 1
background += [fill(rect(0, 78, 108, 5), WOOD), fill(rect(0, 83, 108, 2), WOOD_DARK)]

# ---------------------------------------------------------------- the gnome (on the app's 100 x 100 grid)
gnome = [
    # Tunic
    fill("M40,55 L60,55 L67,90 Q50,93 33,90 Z", TUNIC),
    # Arms going to the book
    stroke("M41,60 L36.5,72", SLEEVE, 4.5),
    stroke("M59,60 L63.5,72", SLEEVE, 4.5),
    # Face
    fill(circle(50, 44, 9), SKIN),
    # Beard
    fill("M40,46 Q36,64 50,74 Q64,64 60,46 Q50,52 40,46 Z", BEARD),
    stroke("M40,46 Q36,64 50,74 Q64,64 60,46 Q50,52 40,46 Z", BEARD_EDGE, 0.8),
    # Happy eyes ^ ^
    stroke("M44.3,43.3 Q46,40.6 47.7,43.3", EYES, 1.1),
    stroke("M52.3,43.3 Q54,40.6 55.7,43.3", EYES, 1.1),
    # Pink cheeks
    fill(circle(43.2, 47, 2.4), BLUSH, 0.55),
    fill(circle(56.8, 47, 2.4), BLUSH, 0.55),
    # Big round nose
    fill(circle(50, 46.5, 3.6), NOSE),
    # Pointy hat, slightly bent
    fill("M38,39 Q42,20 53,7 Q56,22 62,39 Z", HAT),
    fill(rrect(37, 35.5, 26, 5, 2.5), HAT_BAND),
    # Open book
    fill(rrect(35.5, 64.5, 29, 15.5, 1.5), BOOK),
    fill(rect(37.2, 65.6, 12.5, 13.2), PAGE),
    fill(rect(50.3, 65.6, 12.5, 13.2), PAGE),
]
for k in range(4):
    y = 68.6 + k * 2.6
    gnome.append(stroke(f"M39.2,{y} L47.8,{y}", TEXT_LINE, 0.7))
    gnome.append(stroke(f"M52.2,{y} L60.8,{y}", TEXT_LINE, 0.7))
gnome += [fill(circle(36.5, 72, 2.8), SKIN), fill(circle(63.5, 72, 2.8), SKIN)]

GNOME_SCALE = 0.86
GNOME_TX = 54 - 50 * GNOME_SCALE      # centered
GNOME_TY = 15.5

# Little sparkles (108 grid)
sparkles = [fill(star(30, 36, 3.6), AMBER), fill(star(79, 30, 2.8), AMBER), fill(star(83, 45, 1.8), AMBER, 0.8)]

# ---------------------------------------------------------------- writers
def android_shape(s):
    kind, d, color, extra = s
    if kind == "fill":
        a = f'\n        android:fillAlpha="{extra}"' if extra is not None else ""
        return f'    <path\n        android:fillColor="{color}"{a}\n        android:pathData="{d}" />'
    return (f'    <path\n        android:strokeColor="{color}"\n        android:strokeWidth="{extra}"\n'
            f'        android:strokeLineCap="round"\n        android:strokeLineJoin="round"\n        android:pathData="{d}" />')

def android_vector(body, comment, size="108dp"):
    return (f'<?xml version="1.0" encoding="utf-8"?>\n<!-- {comment}\n     Generated: same drawing as the gnome in the app (ui/components/Gnome.kt). -->\n'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{size}"\n    android:height="{size}"\n'
            f'    android:viewportWidth="108"\n    android:viewportHeight="108">\n{body}\n</vector>\n')

def android_group(shapes):
    inner = "\n".join("    " + line for s in shapes for line in android_shape(s).split("\n"))
    return (f'    <group\n        android:scaleX="{GNOME_SCALE}"\n        android:scaleY="{GNOME_SCALE}"\n'
            f'        android:translateX="{GNOME_TX:.2f}"\n        android:translateY="{GNOME_TY}">\n{inner}\n    </group>')

def svg_shape(s):
    kind, d, color, extra = s
    if kind == "fill":
        a = f' fill-opacity="{extra}"' if extra is not None else ""
        return f'<path d="{d}" fill="{color}"{a}/>'
    return f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{extra}" stroke-linecap="round" stroke-linejoin="round"/>'

def svg(clip_round):
    clip = '<clipPath id="c"><path d="' + circle(54, 54, 36) + '"/></clipPath>' if clip_round else ""
    # Round preview: show only the part a round launcher shows (72 of 108, zoomed)
    view = "18 18 72 72" if clip_round else "15 13 78 78"   # iOS: zoomed on the gnome
    g = "".join(svg_shape(s) for s in background + sparkles)
    g += f'<g transform="translate({GNOME_TX:.2f},{GNOME_TY}) scale({GNOME_SCALE})">' + "".join(svg_shape(s) for s in gnome) + "</g>"
    if clip_round:
        g = f'<g clip-path="url(#c)">{g}</g>'
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}" width="1024" height="1024"><defs>{clip}</defs>{g}</svg>'

def write(name, text):
    with open(os.path.join(OUT, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(text)

bg_body = "\n".join(android_shape(s) for s in background)
fg_body = "\n".join(android_shape(s) for s in sparkles) + "\n" + android_group(gnome)
write("ic_launcher_background.xml", android_vector(bg_body, "App icon background: green with a shelf of books."))
write("ic_launcher_foreground.xml", android_vector(fg_body, "App icon foreground: the gnome reading, with sparkles."))
legacy = (f'    <clip-path android:pathData="{circle(54, 54, 50)}" />\n' + bg_body + "\n" + fg_body)
write("ic_launcher.xml", android_vector(legacy, "Round app icon for older Android versions (before 8.0).", size="48dp"))
write("icon_square.svg", svg(False))
write("icon_round_preview.svg", svg(True))
print("ok")
