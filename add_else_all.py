#!/usr/bin/env python3
import re

files = [
    'app/src/main/java/com/soundboost/ui/components/EnhancedAudioVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/ModernVolumeControl.kt',
    'app/src/main/java/com/soundboost/ui/components/PremiumAudioVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/PremiumVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/ThemedPresetButton.kt'
]

for filepath in files:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        new_lines = []
        in_when = False
        brace_count = 0
        when_indent = ''
        
        for i, line in enumerate(lines):
            new_lines.append(line)
            
            # Detect when expression with AppTheme
            if 'when' in line and ('theme' in line.lower() or 'AppTheme' in line):
                in_when = True
                when_indent = line[:len(line) - len(line.lstrip())]
                brace_count = 0
            
            if in_when:
                brace_count += line.count('{') - line.count('}')
                
                # Check if this is the closing brace of when expression
                if brace_count == -1 and '}' in line:
                    # Check if there's already an else branch
                    prev_lines_str = ''.join(new_lines[-20:])
                    if 'else ->' not in prev_lines_str:
                        # Insert else branch before closing brace
                        else_line = when_indent + '        else -> {} // Default case for additional themes\n'
                        new_lines.insert(-1, else_line)
                    in_when = False
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
        
        print(f'✓ Fixed: {filepath}')
        
    except Exception as e:
        print(f'✗ Error in {filepath}: {e}')

print('\nDone!')
