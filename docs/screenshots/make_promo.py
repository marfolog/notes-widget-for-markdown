"""Builds the promo images from raw emulator screenshots.

The raw shots contain a launcher full of unrelated apps, so nothing is used as-is: the two
widgets are cut out and re-composed on a clean background. Everything here is deterministic,
so the images can be regenerated after a UI change by re-running this script.
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont

S = '/private/tmp/claude-501/-Users-marfo-StudioProjects-notes-widget-for-markdown/f0612569-8fa6-49f3-8d25-1ab89435317b/scratchpad'
OUT = '/Users/marfo/StudioProjects/notes-widget-for-markdown/docs/screenshots'

FONT_BOLD = '/System/Library/Fonts/Supplemental/Arial Bold.ttf'
FONT_REG = '/System/Library/Fonts/Supplemental/Arial.ttf'

# Widget bounds inside the 1080x2400 emulator screenshot.
CARD_BOX = (316, 548, 1010, 1136)
LIST_BOX = (316, 1178, 1010, 1766)


def gradient(size, top, bottom):
    w, h = size
    base = Image.new('RGB', (1, h))
    px = base.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        px[0, y] = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return base.resize(size, Image.BICUBIC)


def rounded(img, radius):
    mask = Image.new('L', img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius, fill=255)
    out = img.convert('RGBA')
    out.putalpha(mask)
    return out


def paste_with_shadow(canvas, img, xy, radius=36, blur=26, offset=14, opacity=120):
    shadow = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    x, y = xy
    sd.rounded_rectangle([x, y + offset, x + img.size[0], y + offset + img.size[1]],
                         radius, fill=(0, 0, 0, opacity))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(blur)))
    canvas.alpha_composite(rounded(img, radius), xy)


def cut(name, box):
    return Image.open(f'{S}/{name}').crop(box)


def scaled(img, width):
    ratio = width / img.size[0]
    return img.resize((width, int(img.size[1] * ratio)), Image.LANCZOS)


def promo_pair():
    """Both widgets stacked, with a short explanation beside them."""
    W, H = 1800, 1400
    canvas = gradient((W, H), (231, 238, 250), (206, 219, 242)).convert('RGBA')

    card = scaled(cut('e10.png', CARD_BOX), 700)
    lst = scaled(cut('e10.png', LIST_BOX), 700)

    x = 940
    paste_with_shadow(canvas, card, (x, 120))
    paste_with_shadow(canvas, lst, (x, 150 + card.size[1]))

    d = ImageDraw.Draw(canvas)
    title = ImageFont.truetype(FONT_BOLD, 66)
    sub = ImageFont.truetype(FONT_REG, 34)
    small = ImageFont.truetype(FONT_REG, 30)

    d.text((110, 190), 'Your Markdown notes', font=title, fill=(23, 33, 54))
    d.text((110, 268), 'on the home screen', font=title, fill=(23, 33, 54))

    lines = [
        'Reads plain .md files from a folder you pick.',
        'No account, no backend, no database.',
        '',
        'Two widgets: cards with a Markdown preview,',
        'or a compact list of note names.',
        '',
        'The chip shows what git last did — a pull,',
        'a commit, or a conflict your sync client',
        'walked away from.',
    ]
    y = 400
    for line in lines:
        d.text((110, y), line, font=sub, fill=(58, 71, 96))
        y += 52

    d.text((110, 1230), 'No Android permissions. Not even INTERNET.', font=small, fill=(90, 103, 130))
    canvas.convert('RGB').save(f'{OUT}/promo-01-overview.png')


def promo_phone():
    """A phone-shaped frame containing just the two widgets on a clean wallpaper."""
    PW, PH = 760, 1580
    W, H = 1200, 1800
    canvas = gradient((W, H), (238, 242, 251), (214, 224, 244)).convert('RGBA')

    screen = gradient((PW - 24, PH - 24), (24, 30, 46), (12, 15, 26)).convert('RGBA')

    card = scaled(cut('e10.png', CARD_BOX), PW - 120)
    lst = scaled(cut('e10.png', LIST_BOX), PW - 120)
    sx = 48
    screen.alpha_composite(rounded(card, 30), (sx, 180))
    screen.alpha_composite(rounded(lst, 30), (sx, 210 + card.size[1]))

    sd = ImageDraw.Draw(screen)
    clock = ImageFont.truetype(FONT_REG, 26)
    sd.text((48, 60), '9:41', font=clock, fill=(232, 236, 245))

    body = Image.new('RGBA', (PW, PH), (0, 0, 0, 0))
    bd = ImageDraw.Draw(body)
    bd.rounded_rectangle([0, 0, PW - 1, PH - 1], 74, fill=(18, 20, 28, 255))
    body.alpha_composite(rounded(screen, 62), (12, 12))

    px, py = (W - PW) // 2, (H - PH) // 2
    shadow = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([px, py + 26, px + PW, py + 26 + PH], 74, fill=(0, 0, 0, 110))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(34)))
    canvas.alpha_composite(body, (px, py))
    canvas.convert('RGB').save(f'{OUT}/promo-02-phone.png')


def promo_single(name, box, out_name):
    W, H = 1200, 900
    canvas = gradient((W, H), (233, 239, 251), (209, 221, 243)).convert('RGBA')
    widget = scaled(cut(name, box), 900)
    paste_with_shadow(canvas, widget, ((W - 900) // 2, (H - widget.size[1]) // 2))
    canvas.convert('RGB').save(f'{OUT}/{out_name}')


promo_pair()
promo_phone()
promo_single('e10.png', CARD_BOX, 'promo-03-cards.png')
promo_single('e10.png', LIST_BOX, 'promo-04-list.png')
print('hotovo')
