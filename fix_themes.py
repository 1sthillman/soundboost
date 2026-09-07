#!/usr/bin/env python3
import re
import glob

# Old theme mappings to new themes
old_to_new_themes = {
    'SYSTEM': 'WATER',
    'NEON_LIGHT': 'NEON',
    'NEON_DARK': 'MIDNIGHT',
    'OCEAN_BLUE': 'WATER',
    'SUNSET_ORANGE': 'SUN',
    'FOREST_GREEN': 'FOREST',
    'ROYAL_PURPLE': 'ROYAL'
}

old_to_new_accents = {
    'CYAN': 'WATER_CYAN',
    'PINK': 'NEON_PINK',
    'ORANGE': 'SUN_ORANGE',
    'GREEN': 'FOREST_GREEN',
    'PURPLE': 'ROYAL_GOLD',
    'BLUE': 'WATER_CYAN'
}

def fix_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original = content
        
        # Replace old theme names with new ones
        for old, new in old_to_new_themes.items():
            content = re.sub(rf'AppTheme\.{old}\b', f'AppTheme.{new}', content)
        
        # Replace old accent names with new ones
        for old, new in old_to_new_accents.items():
            content = re.sub(rf'ColorAccent\.{old}\b', f'ColorAccent.{new}', content)
        
        if content != original:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f'Fixed: {filepath}')
            return True
        return False
    except Exception as e:
        print(f'Error processing {filepath}: {e}')
        return False

# Process all Kotlin files
kt_files = glob.glob('app/src/main/java/**/*.kt', recursive=True)
fixed_count = 0

for kt_file in kt_files:
    if fix_file(kt_file):
        fixed_count += 1

print(f'\nFixed {fixed_count} files')
