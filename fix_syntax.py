#!/usr/bin/env python3

files = [
    'app/src/main/java/com/soundboost/ui/components/ModernVolumeControl.kt',
    'app/src/main/java/com/soundboost/ui/components/PremiumAudioVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/PremiumVisualizer.kt',
    'app/src/main/java/com/soundboost/ui/components/ThemedPresetButton.kt'
]

for filepath in files:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Remove incorrectly placed else branches
        lines = content.split('\n')
        new_lines = []
        skip_next = False
        
        for i, line in enumerate(lines):
            if skip_next:
                skip_next = False
                continue
                
            # Check if this line has just a closing brace
            if line.strip() == '}' and i + 1 < len(lines):
                next_line = lines[i + 1]
                # If next line is the misplaced else
                if 'else -> {}' in next_line and '// Default case' in next_line:
                    # Replace closing brace with else branch + closing brace
                    indent = line[:len(line) - len(line.lstrip())]
                    new_lines.append(indent + '    else -> {} // Default')
                    new_lines.append(line)
                    skip_next = True
                    continue
            
            new_lines.append(line)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(new_lines))
        
        print(f'✓ Fixed: {filepath}')
        
    except Exception as e:
        print(f'✗ Error: {filepath}: {e}')

print('Done!')
