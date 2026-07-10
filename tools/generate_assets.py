from pathlib import Path
from PIL import Image, ImageDraw
import re, json, hashlib, math, wave, struct, subprocess, os, html
root=Path(__file__).resolve().parents[1]
java=(root/'src/main/java/com/tre/sololeveling/registry/ModItems.java').read_text()
ids=[]
for m in re.finditer(r'(?:weapon|story|accessory|rune|add)\("([a-z0-9_]+)"', java):
    if m.group(1) not in ids: ids.append(m.group(1))
res=root/'src/main/resources'
itemtex=res/'assets/sololeveling/textures/item'; itemtex.mkdir(parents=True,exist_ok=True)
models=res/'assets/sololeveling/models/item'; models.mkdir(parents=True,exist_ok=True)
langdir=res/'assets/sololeveling/lang'; langdir.mkdir(parents=True,exist_ok=True)
armor=res/'assets/sololeveling/textures/models/armor'; armor.mkdir(parents=True,exist_ok=True)
gui=res/'assets/sololeveling/textures/gui'; gui.mkdir(parents=True,exist_ok=True)
sounddir=res/'assets/sololeveling/sounds'; sounddir.mkdir(parents=True,exist_ok=True)
tools=root/'tools/pixel_art/items'; tools.mkdir(parents=True,exist_ok=True)

PAL=[(7,10,20,255),(12,21,45,255),(32,24,66,255),(74,42,135,255),(122,74,255,255),(54,221,255,255),(140,238,255,255),(195,205,218,255),(82,91,110,255),(247,250,255,255),(174,31,62,255),(255,125,43,255),(74,192,127,255)]

def seed(id): return int(hashlib.sha256(id.encode()).hexdigest()[:8],16)
def human(id):
    special={'kasakas':'Kasaka’s','kamishs':'Kamish’s','barukas':'Baruka’s','assassins':'Assassin’s','gatekeepers':'Gatekeeper’s','magicians':'Magician’s','kings':'King’s','monarchs':'Monarch’s','seekers':'Seeker’s','sangshiks':'Sangshik’s'}
    words=id.split('_')
    return ' '.join(special.get(w, w.capitalize()) for w in words)

def palette_for(id):
    s=seed(id); accent=PAL[3+(s%4)]
    if any(x in id for x in ['venom','kasaka']): accent=PAL[12]
    if 'mana' in id or 'teleport' in id: accent=PAL[5]
    if 'healing' in id or 'life' in id: accent=PAL[10]
    if 'demon' in id or 'cursed' in id: accent=PAL[11]
    if 'shadow' in id or 'black_heart' in id: accent=PAL[4]
    return [PAL[0],PAL[1],PAL[8],PAL[7],accent,PAL[9]]

def pixels_for(id,n=16):
    p=palette_for(id); img=Image.new('RGBA',(n,n),(0,0,0,0)); d=ImageDraw.Draw(img)
    weapon=any(x in id for x in ['dagger','sword','fang']) and not any(x in id for x in ['cerberus','raikan'])
    armoritem=any(x in id for x in ['chestplate','coat','top','trousers','pants','boots','shoes','helmet','gloves','gauntlets'])
    potion='potion' in id or 'water_of_life' in id
    rune='rune' in id
    key='key' in id
    box='box' in id
    heart='heart' in id
    if weapon:
        flip='right' in id
        pts=[(4,13),(6,11),(6,5),(8,2),(10,1),(9,4),(8,7),(8,11),(10,13),(8,15),(6,14)]
        if flip: pts=[(n-1-x,y) for x,y in pts]
        d.polygon(pts,fill=p[3]); d.line(pts+[pts[0]],fill=p[2],width=1)
        d.line([(7 if not flip else 8,11),(10 if not flip else 5,14)],fill=p[4],width=2)
        d.point((8 if not flip else 7,3),fill=p[5])
    elif armoritem:
        if 'helmet' in id:
            d.polygon([(4,4),(6,2),(10,2),(12,4),(12,10),(10,12),(6,12),(4,10)],fill=p[2]); d.rectangle((5,5,11,8),fill=p[3]); d.rectangle((6,6,10,6),fill=p[4])
        elif any(x in id for x in ['boots','shoes']):
            d.polygon([(4,3),(8,3),(8,11),(12,12),(12,14),(4,14)],fill=p[2]); d.line([(5,4),(7,10),(11,12)],fill=p[4],width=1)
        elif any(x in id for x in ['gloves','gauntlets']):
            d.polygon([(5,2),(10,2),(12,5),(10,12),(7,14),(4,11),(4,5)],fill=p[2]); d.line([(6,3),(7,11)],fill=p[4],width=2)
        elif any(x in id for x in ['pants','trousers']):
            d.polygon([(4,2),(12,2),(11,8),(10,14),(7,14),(8,8),(6,14),(3,14),(4,8)],fill=p[2]); d.line([(5,3),(11,3)],fill=p[4],width=1)
        else:
            d.polygon([(4,2),(7,3),(9,3),(12,2),(14,6),(12,8),(11,14),(5,14),(4,8),(2,6)],fill=p[2]); d.line([(4,3),(8,6),(12,3)],fill=p[4],width=2)
    elif potion:
        d.rectangle((6,1,9,3),fill=p[3]); d.rectangle((5,3,10,5),fill=p[2]); d.polygon([(4,5),(11,5),(13,9),(11,14),(4,14),(2,9)],fill=p[3]); d.polygon([(4,8),(11,8),(11,13),(4,13),(3,10)],fill=p[4]); d.point((8,9),fill=p[5])
    elif rune:
        d.polygon([(8,1),(13,5),(12,12),(8,15),(3,12),(2,5)],fill=p[2]); d.line([(8,2),(8,13),(4,9),(12,5)],fill=p[4],width=1); d.point((8,7),fill=p[5])
    elif key:
        d.ellipse((2,2,8,8),outline=p[4],width=2); d.line([(7,7),(13,13)],fill=p[3],width=2); d.line([(10,10),(12,8)],fill=p[3],width=1); d.line([(12,12),(14,10)],fill=p[3],width=1)
    elif box:
        d.rectangle((2,4,13,13),fill=p[2],outline=p[3]); d.rectangle((1,3,14,6),fill=p[3]); d.line([(8,3),(8,13)],fill=p[4],width=2); d.point((8,8),fill=p[5])
    elif heart:
        d.polygon([(8,14),(2,7),(2,4),(4,2),(7,3),(8,5),(9,3),(12,2),(14,4),(14,7)],fill=p[4]); d.line([(8,5),(8,13)],fill=p[5],width=1)
    elif any(x in id for x in ['necklace','ring','earring','orb']):
        d.ellipse((3,3,12,12),outline=p[3],width=2); d.ellipse((6,6,9,9),fill=p[4]); d.point((8,7),fill=p[5])
    elif any(x in id for x in ['stone','crystal','essence','venom']):
        d.polygon([(8,1),(13,6),(11,13),(6,15),(2,9),(4,3)],fill=p[3]); d.polygon([(8,2),(10,7),(8,13),(5,8)],fill=p[4]); d.point((8,4),fill=p[5])
    else:
        d.polygon([(8,1),(13,5),(12,11),(8,15),(3,11),(2,5)],fill=p[2]); d.line([(4,5),(8,3),(11,6),(8,13)],fill=p[4],width=2)
    return img

lang={
 'itemGroup.sololeveling':'Solo Leveling: The Player','screen.sololeveling.system':'THE SYSTEM','key.categories.sololeveling':'Solo Leveling',
 'key.sololeveling.system':'Open the System','key.sololeveling.primary':'Primary Ability','key.sololeveling.secondary':'Secondary Ability','key.sololeveling.extract':'Shadow Extraction','key.sololeveling.shadows':'Shadow Army','key.sololeveling.exchange':'Shadow Exchange','key.sololeveling.quicksilver':'Quicksilver','key.sololeveling.authority':'Ruler’s Authority','key.sololeveling.hud':'Toggle System HUD','key.sololeveling.dodge':'Dodge',
 'subtitles.sololeveling.system':'The System responds','subtitles.sololeveling.level_up':'Hunter level increased','subtitles.sololeveling.quest_complete':'Quest completed','subtitles.sololeveling.ability':'Ability activated','subtitles.sololeveling.shadow':'Shadow energy rises','subtitles.sololeveling.mana_fail':'Insufficient mana'
}
manifest=[]
weapon_ids=[]
for id in ids:
    img=pixels_for(id); path=itemtex/f'{id}.png'; img.save(path)
    parent='minecraft:item/handheld' if any(x in id for x in ['dagger','sword','fang']) and id not in ['raikan_fang','cerberus_fang'] else 'minecraft:item/generated'
    (models/f'{id}.json').write_text(json.dumps({'parent':parent,'textures':{'layer0':f'sololeveling:item/{id}'}},indent=2))
    lang[f'item.sololeveling.{id}']=human(id)
    if parent.endswith('handheld'): weapon_ids.append(id)
    pal=palette_for(id)
    pix=list(img.getdata())
    arr=[]
    for y in range(16):
        row=[]
        for x in range(16):
            rgba=pix[y*16+x]; row.append('#%02x%02x%02x%02x'%rgba if rgba[3] else '')
        arr.append(row)
    js=json.dumps(arr,separators=(',',':'))
    palette_strings=['#%02x%02x%02x'%c[:3] for c in pal]
    page=f'''<!doctype html><html><head><meta charset="utf-8"><title>{html.escape(human(id))} Pixel Art</title><style>body{{background:#07101f;color:#dffaff;font-family:system-ui;text-align:center}}canvas{{image-rendering:pixelated;width:512px;height:512px;background:repeating-conic-gradient(#15213a 0 25%,#0e182d 0 50%) 50%/24px 24px;border:2px solid #35d9ff}}button{{background:#14264d;color:#fff;border:1px solid #35d9ff;padding:10px 18px}}code{{display:block;margin:10px}}</style></head><body><h1>{html.escape(human(id))}</h1><canvas id="c" width="16" height="16"></canvas><code>Palette: {', '.join(palette_strings)}</code><button id="save">Export PNG</button><script>const px={js};const c=document.getElementById('c'),x=c.getContext('2d');x.imageSmoothingEnabled=false;for(let y=0;y<16;y++)for(let i=0;i<16;i++)if(px[y][i]){{x.fillStyle=px[y][i];x.fillRect(i,y,1,1)}}document.getElementById('save').onclick=()=>{{const a=document.createElement('a');a.download='{id}.png';a.href=c.toDataURL('image/png');a.click()}};</script></body></html>'''
    (tools/f'{id}.html').write_text(page)
    manifest.append(f'- `assets/sololeveling/textures/item/{id}.png` — `{human(id)}` inventory texture; editable source `tools/pixel_art/items/{id}.html`')

# armor sheets
for material,base,accent in [('hunter',(34,40,52,255),(95,110,130,255)),('truth_seeker',(8,18,36,255),(45,210,238,255)),('shadow_monarch',(8,6,18,255),(126,70,255,255))]:
    for layer in (1,2):
        im=Image.new('RGBA',(64,32),(0,0,0,0)); d=ImageDraw.Draw(im)
        # fill standard armor UV islands broadly; transparent areas are harmless
        d.rectangle((0,0,63,31),fill=base)
        for y in range(0,32,4): d.line((0,y,63,y),fill=(max(0,base[0]-4),max(0,base[1]-4),max(0,base[2]-4),255))
        for x in range(2+(layer-1),64,8): d.line((x,0,x,31),fill=accent,width=1)
        d.rectangle((0,0,63,31),outline=(10,10,18,255))
        im.save(armor/f'{material}_layer_{layer}.png')

# GUI panel/button/bar textures
for name,size,base,accent in [('system_panel',(256,256),(7,14,35,220),(54,221,255,255)),('button',(64,20),(17,34,72,255),(54,221,255,255)),('button_hover',(64,20),(31,51,96,255),(122,74,255,255)),('hud_bar',(128,8),(10,17,38,255),(54,221,255,255))]:
    im=Image.new('RGBA',size,base); d=ImageDraw.Draw(im); d.rectangle((0,0,size[0]-1,size[1]-1),outline=accent,width=1); im.save(gui/f'{name}.png')

# ability, rank, quest icons
for folder,names in [('ability',['stealth','bloodlust','quicksilver','mutilation','dagger_rush','rulers_authority','dragons_fear','shadow_extraction','shadow_preservation','shadow_exchange','monarch_domain']),('rank',['e','d','c','b','a','s','national','shadow_monarch']),('quest',['daily','main','training','emergency','penalty'])]:
    out=gui/folder; out.mkdir(parents=True,exist_ok=True)
    for name in names: pixels_for(folder+'_'+name).resize((32,32),Image.Resampling.NEAREST).save(out/f'{name}.png')

# logo
logo=Image.new('RGBA',(256,256),(0,0,0,0)); d=ImageDraw.Draw(logo); d.polygon([(128,18),(224,74),(210,186),(128,238),(46,186),(32,74)],fill=(8,16,39,255),outline=(54,221,255,255),width=5); d.polygon([(128,49),(186,84),(175,166),(128,201),(81,166),(70,84)],outline=(122,74,255,255),width=7); d.text((96,103),'SL',fill=(230,250,255,255),stroke_width=1,stroke_fill=(54,221,255,255)); logo.save(res/'sololeveling_logo.png')

# language
(langdir/'en_us.json').write_text(json.dumps(lang,ensure_ascii=False,indent=2))

# sounds and sounds.json
sounds={
 'system':{'subtitle':'subtitles.sololeveling.system'},'level_up':{'subtitle':'subtitles.sololeveling.level_up'},'quest_complete':{'subtitle':'subtitles.sololeveling.quest_complete'},
 'ability':{'subtitle':'subtitles.sololeveling.ability'},'shadow':{'subtitle':'subtitles.sololeveling.shadow'},'mana_fail':{'subtitle':'subtitles.sololeveling.mana_fail'}
}
for idx,(name,meta) in enumerate(sounds.items()):
    wav=Path('/tmp')/f'{name}.wav'; rate=44100; dur=0.30+idx*0.035
    with wave.open(str(wav),'w') as w:
        w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate)
        frames=[]
        for i in range(int(rate*dur)):
            t=i/rate; env=min(1,t/0.02)*max(0,1-t/dur)
            f=300+idx*65 + (170*t/dur if name in ('level_up','system') else 0)
            val=0.24*env*(math.sin(2*math.pi*f*t)+0.35*math.sin(2*math.pi*(f*1.5)*t))
            frames.append(struct.pack('<h',int(max(-1,min(1,val))*32767)))
        w.writeframes(b''.join(frames))
    subprocess.run(['ffmpeg','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(sounddir/f'{name}.ogg')],check=True)
    meta['sounds']=[{'name':f'sololeveling:{name}','stream':False}]
(res/'assets/sololeveling/sounds.json').write_text(json.dumps(sounds,indent=2))

# item tags
(tagdir:=res/'data/sololeveling/tags/items').mkdir(parents=True,exist_ok=True)
(tagdir/'daggers.json').write_text(json.dumps({'replace':False,'values':[f'sololeveling:{x}' for x in weapon_ids if 'sword' not in x and 'longsword' not in x]},indent=2))

# recipes
recipes=res/'data/sololeveling/recipes'; recipes.mkdir(parents=True,exist_ok=True)
recipe_defs={
 'healing_potion': ['minecraft:glass_bottle','minecraft:sweet_berries'],
 'mana_potion':['minecraft:glass_bottle','minecraft:lapis_lazuli'],
 'greater_healing_potion':['sololeveling:healing_potion','minecraft:glistering_melon_slice'],
 'greater_mana_potion':['sololeveling:mana_potion','minecraft:amethyst_shard']
}
for out,ings in recipe_defs.items():
    (recipes/f'{out}.json').write_text(json.dumps({'type':'minecraft:crafting_shapeless','ingredients':[{'item':x} for x in ings],'result':{'item':f'sololeveling:{out}','count':1}},indent=2))

# HTML index
links='\n'.join(f'<li><a href="items/{id}.html">{html.escape(human(id))}</a></li>' for id in ids)
(root/'tools/pixel_art/index.html').write_text(f'<!doctype html><html><head><meta charset="utf-8"><title>Solo Leveling Pixel Art</title><style>body{{background:#07101f;color:#e6fbff;font-family:system-ui;max-width:900px;margin:auto}}a{{color:#35d9ff}}li{{margin:8px}}</style></head><body><h1>Solo Leveling: The Player — Pixel Art Sources</h1><ul>{links}</ul></body></html>')

# asset manifest
(root/'ASSET_MANIFEST.md').write_text('# Asset Manifest\n\nAll art and sounds are original procedural assets created for this project.\n\n## Items\n\n'+'\n'.join(manifest)+'\n\n## Armor\n\n- Hunter, Truth Seeker, and Shadow Monarch layer 1/layer 2 textures.\n\n## Interface\n\n- System panel, normal/hover buttons, HUD bar, ability icons, quest icons, and rank icons.\n\n## Sounds\n\n- system, level_up, quest_complete, ability, shadow, mana_fail.\n')
print('generated',len(ids),'items')
