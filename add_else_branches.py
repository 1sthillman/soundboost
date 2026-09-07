#!/usr/bin/env python3
import re

files_to_fix = [
    'app/src/main/java/com/soundboost/ui/components/AnimatedThemeSelector.kt',
    'app/src/main/java/com/soundboost/ui/components/EnhancedAudioVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/ModernVolumeControl.kt',
    'app/src/main/java/com/soundboost/ui/components/PremiumAudioVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/PremiumVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/ThemedPresetButton.kt'
]

def add_else_to_when(content):
    # Pattern to find when expressions without else
    # Find closing brace of last branch, then add else before final closing brace
    
    # Add else for theme when expressions (look for ROYAL pattern followed by })
    content = re.sub(
        r'(AppTheme\.ROYAL\s*->.*?\n\s+\})\s*\n(\s+)\}',
        r'\1\n\2    else -> {} // Fallback for new themes\n\2}',
        content,
        flags=re.DOTALL
    )
    
    # Add else for ColorAccent when expressions  
    content = re.sub(
        r'(ColorAccent\.ROYAL_GOLD\s*->.*?\n\s+)\}',
        r'\1    else -> Color(0xFF33d9e8) // Default fallback\n    }',
        content,
        flags=re.DOTALL
    )
    
    return content

for filepath in files_to_fix:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        new_content = add_else_to_when(content)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f'Processed: {filepath}')
    except Exception as e:
        print(f'Error: {filepath}: {e}')

print('Done!')
