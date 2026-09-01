"""Builds the store and Reddit promo images from raw emulator screenshots.

The raw shots contain a launcher full of unrelated apps, so nothing is used as-is: the widgets
are cut out at their exact bounds and re-composed on a clean background. Everything is
deterministic, so the set can be regenerated after a UI change by re-capturing the emulator
screenshot and re-running this file.

Sizes follow what stores expect: a 1024x500 feature graphic and 1080x1920 (9:16) screenshots.
"""
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

SRC = '/private/tmp/claude-501/-Users-marfo-StudioProjects-notes-widget-for-markdown/f0612569-8fa6-49f3-8d25-1ab89435317b/scratchpad'
OUT = '/Users/marfo/StudioProjects/notes-widget-for-markdown/docs/screenshots'
SHOT = 'k1.png'

FONT_BOLD = '/System/Library/Fonts/Supplemental/Arial Bold.ttf'
FONT_REG = '/System/Library/Fonts/Supplemental/Arial.ttf'

# Widget bounds measured in the 1080x2400 screenshot, and the widget's own corner radius.
CARD_BOX = (56, 231, 1024, 1124)
LIST_BOX = (56, 1167, 1024, 1748)
WIDGET_RADIUS = 58

INK_TOP = (20, 26, 46)
INK_BOTTOM = (31, 42, 71)
HEADLINE = (242, 244, 250)
SUBTLE = (155, 166, 196)
GLYPH = (38, 49, 79)
ACCENT = (240, 184, 96)


def gradient(size, top, bottom):
    w, h = size
    strip = Image.new('RGB', (1, h))
    px = strip.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        px[0, y] = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return strip.resize(size, Image.BICUBIC)


def rounded(img, radius):
    mask = Image.new('L', img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius, fill=255)
    out = img.convert('RGBA')
    out.putalpha(mask)
    return out


def widget(box, width):
    """Cuts a widget out at its real bounds and rounds it with its real corner radius."""
    crop = Image.open(f'{SRC}/{SHOT}').convert('RGBA').crop(box)
    scale = width / crop.size[0]
    img = crop.resize((width, int(crop.size[1] * scale)), Image.LANCZOS)
    return rounded(img, int(WIDGET_RADIUS * scale))


def perspective_coeffs(src_quad, dst_quad):
    """Solves the 8 coefficients mapping dst -> src, as Image.PERSPECTIVE expects."""
    rows = []
    rhs = []
    for (dx, dy), (sx, sy) in zip(dst_quad, src_quad):
        rows.append([dx, dy, 1, 0, 0, 0, -sx * dx, -sx * dy])
        rhs.append(sx)
        rows.append([0, 0, 0, dx, dy, 1, -sy * dx, -sy * dy])
        rhs.append(sy)

    # Gaussian elimination — avoids a numpy dependency for eight unknowns.
    n = 8
    for col in range(n):
        pivot = max(range(col, n), key=lambda r: abs(rows[r][col]))
        rows[col], rows[pivot] = rows[pivot], rows[col]
        rhs[col], rhs[pivot] = rhs[pivot], rhs[col]
        div = rows[col][col]
        rows[col] = [v / div for v in rows[col]]
        rhs[col] /= div
        for r in range(n):
            if r == col:
                continue
            factor = rows[r][col]
            if factor:
                rows[r] = [v - factor * rows[col][i] for i, v in enumerate(rows[r])]
                rhs[r] -= factor * rhs[col]
    return rhs


def tilt(img, squeeze=0.88, shear=0.035):
    """Leans the image away from the viewer on its right edge."""
    w, h = img.size
    dy = h * (1 - squeeze) / 2
    sh = h * shear
    src = [(0, 0), (w, 0), (w, h), (0, h)]
    dst = [(0, 0), (w, dy + sh), (w, h - dy + sh), (0, h)]
    return img.transform((w, int(h + sh)), Image.PERSPECTIVE,
                         perspective_coeffs(src, dst), Image.BICUBIC)


def shadow_of(canvas, img, xy, blur=44, offset=(20, 36), opacity=140):
    """Shadow traced from the image's own alpha, so it follows a tilted silhouette."""
    alpha = img.split()[3].point(lambda v: v * opacity // 255)
    layer = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    layer.paste(Image.new('RGBA', img.size, (0, 0, 0, 255)),
                (xy[0] + offset[0], xy[1] + offset[1]), alpha)
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def phone(card_w, with_list=True, scale=1.0):
    """A phone body holding one or both widgets, built at 2x and downsampled for clean edges."""
    ss = 2
    pw, ph = int(card_w * 1.36) * ss, int(card_w * 2.72) * ss
    body = Image.new('RGBA', (pw, ph), (0, 0, 0, 0))
    ImageDraw.Draw(body).rounded_rectangle([0, 0, pw - 1, ph - 1], int(64 * ss), fill=(16, 18, 26, 255))

    screen = gradient((pw - 20 * ss, ph - 20 * ss), (22, 27, 44), (13, 16, 28)).convert('RGBA')
    inner_w = screen.size[0]
    card = widget(CARD_BOX, int(inner_w * 0.88))
    x = (inner_w - card.size[0]) // 2
    screen.alpha_composite(card, (x, int(150 * ss)))
    if with_list:
        lst = widget(LIST_BOX, card.size[0])
        screen.alpha_composite(lst, (x, int(150 * ss) + card.size[1] + int(40 * ss)))

    ImageDraw.Draw(screen).text((int(46 * ss), int(56 * ss)), '9:41',
                                font=ImageFont.truetype(FONT_REG, int(26 * ss)), fill=(226, 231, 242))
    body.alpha_composite(rounded(screen, int(52 * ss)), (10 * ss, 10 * ss))

    target = (int(pw / ss * scale), int(ph / ss * scale))
    return body.resize(target, Image.LANCZOS)


def glyph_texture(canvas):
    """Oversized Markdown marks, barely lighter than the background — texture, not decoration."""
    d = ImageDraw.Draw(canvas)
    w, h = canvas.size
    marks = [('#', 0.06, 0.55, 0.42), ('- [ ]', 0.55, 0.06, 0.17), ('>', 0.78, 0.62, 0.34)]
    for text, fx, fy, fs in marks:
        d.text((int(w * fx), int(h * fy)), text,
               font=ImageFont.truetype(FONT_BOLD, int(h * fs)), fill=GLYPH)


def headline(canvas, lines, sub_lines, top_frac=0.09, size_div=10.5):
    d = ImageDraw.Draw(canvas)
    w, h = canvas.size
    margin = int(w * 0.06)
    y = int(h * top_frac)
    f = ImageFont.truetype(FONT_BOLD, int(w / size_div))
    for line in lines:
        d.text((margin, y), line, font=f, fill=HEADLINE)
        y += int(f.size * 1.12)
    if sub_lines:
        y += int(f.size * 0.30)
        fs = ImageFont.truetype(FONT_REG, int(f.size * 0.42))
        for line in sub_lines:
            d.text((margin, y), line, font=fs, fill=SUBTLE)
            y += int(fs.size * 1.35)


def canvas_of(size):
    c = gradient(size, INK_TOP, INK_BOTTOM).convert('RGBA')
    glyph_texture(c)
    return c


def screenshot_hero():
    c = canvas_of((1080, 1920))
    headline(c, ['Your .md files.', 'On the home screen.'],
             ['Reads a folder you pick. No account,', 'no backend, no database.'])
    ph = tilt(phone(430))
    shadow_of(c, ph, ((1080 - ph.size[0]) // 2, 700))
    c.alpha_composite(ph, ((1080 - ph.size[0]) // 2, 700))
    c.convert('RGB').save(f'{OUT}/store-01-hero.png')


def screenshot_widget(box, out_name, lines, sub_lines):
    c = canvas_of((1080, 1920))
    headline(c, lines, sub_lines)
    img = widget(box, 940)
    y = 780
    shadow_of(c, img, (70, y), blur=36, offset=(0, 26))
    c.alpha_composite(img, (70, y))
    c.convert('RGB').save(f'{OUT}/{out_name}')


def screenshot_claim():
    c = canvas_of((1080, 1920))
    d = ImageDraw.Draw(c)
    f = ImageFont.truetype(FONT_BOLD, 118)
    fs = ImageFont.truetype(FONT_REG, 46)
    d.text((70, 620), 'Zero', font=f, fill=HEADLINE)
    d.text((70, 760), 'permissions.', font=f, fill=ACCENT)
    d.text((70, 940), 'Not even INTERNET.', font=ImageFont.truetype(FONT_BOLD, 74), fill=HEADLINE)
    for i, line in enumerate([
        'The app cannot phone home, because Android',
        'never grants it the ability to. Check the manifest,',
        'or run aapt dump permissions on the APK.',
    ]):
        d.text((70, 1120 + i * 66), line, font=fs, fill=SUBTLE)
    c.convert('RGB').save(f'{OUT}/store-05-permissions.png')


def feature_graphic():
    c = canvas_of((1024, 500))
    d = ImageDraw.Draw(c)
    d.text((60, 150), 'Markdown notes', font=ImageFont.truetype(FONT_BOLD, 76), fill=HEADLINE)
    d.text((60, 240), 'on your home screen', font=ImageFont.truetype(FONT_BOLD, 76), fill=HEADLINE)
    d.text((62, 350), 'Two widgets, git sync status, no permissions.',
           font=ImageFont.truetype(FONT_REG, 34), fill=SUBTLE)

    back = ImageEnhance.Brightness(tilt(phone(230, with_list=False), squeeze=1.12, shear=-0.03)).enhance(0.72)
    front = tilt(phone(260))
    shadow_of(c, back, (600, 90), blur=30, opacity=110)
    c.alpha_composite(back, (600, 90))
    shadow_of(c, front, (760, 60))
    c.alpha_composite(front, (760, 60))
    c.convert('RGB').save(f'{OUT}/store-00-feature.png')


screenshot_hero()
screenshot_widget(CARD_BOX, 'store-02-cards.png',
                  ['Notes, not just', 'note titles.'],
                  ['Headings, links, lists and task checkboxes,', 'rendered on the widget itself.'])
screenshot_widget(LIST_BOX, 'store-03-list.png',
                  ['Or a compact', 'list of names.'],
                  ['Same folder, denser view.', 'Tap any note to open it.'])
screenshot_widget(CARD_BOX, 'store-04-sync.png',
                  ['Know when sync', 'broke.'],
                  ['The chip reads .git directly: last pull,', 'a stuck merge, or unpushed work.'])
screenshot_claim()
feature_graphic()
print('hotovo')
