#!/usr/bin/env python3
"""Fix all strings.xml files that have gain warning strings after </resources> tag"""

import os
import re

# List of language codes to fix
languages = ['de', 'es', 'fr', 'ja', 'ko', 'ru', 'zh']

for lang in languages:
    filepath = f'app/src/main/res/values-{lang}/strings.xml'
    
    if not os.path.exists(filepath):
        print(f"❌ {filepath} not found")
        continue
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if file has the problem (</resources> followed by more strings)
    if '</resources>' in content and content.index('</resources>') < len(content) - 50:
        print(f"🔧 Fixing {filepath}...")
        
        # Split at </resources>
        parts = content.split('</resources>')
        
        # Get the main content before </resources>
        main_content = parts[0]
        
        # Get everything after </resources> (the orphaned strings)
        orphaned_content = '</resources>'.join(parts[1:])
        
        # Remove empty lines and extra whitespace from orphaned content
        orphaned_content = orphaned_content.strip()
        
        # If there's orphaned content, add it before </resources>
        if orphaned_content:
            # Remove leading whitespace/comments if any
            orphaned_content = re.sub(r'^\s*<!--.*?-->\s*', '', orphaned_content, flags=re.DOTALL)
            orphaned_content = orphaned_content.strip()
            
            if orphaned_content:
                # Add proper indentation
                lines = orphaned_content.split('\n')
                indented_lines = []
                for line in lines:
                    if line.strip():
                        if not line.startswith('    '):
                            indented_lines.append('    ' + line.lstrip())
                        else:
                            indented_lines.append(line)
                    else:
                        indented_lines.append(line)
                
                orphaned_content = '\n'.join(indented_lines)
                
                # Reconstruct the file
                new_content = main_content.rstrip() + '\n' + orphaned_content + '\n</resources>\n'
            else:
                new_content = main_content.rstrip() + '\n</resources>\n'
        else:
            new_content = main_content.rstrip() + '\n</resources>\n'
        
        # Write back
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"✅ Fixed {filepath}")
    else:
        print(f"✓ {filepath} is OK")

print("\n✨ Done!")
