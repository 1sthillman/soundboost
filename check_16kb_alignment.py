#!/usr/bin/env python3
"""
Check 16 KB alignment for all .so files in an AAB/APK
Checks both ELF PT_LOAD p_align and ZIP alignment
"""

import struct
import zipfile
import sys
from pathlib import Path

def read_elf_program_headers(data):
    """Parse ELF program headers and return PT_LOAD alignments"""
    # ELF header
    if data[:4] != b'\x7fELF':
        return None, "Not an ELF file"
    
    ei_class = data[4]  # 1=32bit, 2=64bit
    ei_data = data[5]   # 1=little endian, 2=big endian
    
    if ei_class != 2:  # Only 64-bit
        return None, "Not 64-bit ELF"
    
    if ei_data != 1:  # Only little endian
        return None, "Not little endian"
    
    # Parse 64-bit little endian ELF
    # e_phoff at offset 32 (8 bytes)
    # e_phentsize at offset 54 (2 bytes)
    # e_phnum at offset 56 (2 bytes)
    
    e_phoff = struct.unpack('<Q', data[32:40])[0]
    e_phentsize = struct.unpack('<H', data[54:56])[0]
    e_phnum = struct.unpack('<H', data[56:58])[0]
    
    pt_load_alignments = []
    
    for i in range(e_phnum):
        ph_offset = e_phoff + (i * e_phentsize)
        
        # Program header structure (64-bit)
        # p_type at offset 0 (4 bytes)
        # p_flags at offset 4 (4 bytes)
        # p_offset at offset 8 (8 bytes)
        # p_vaddr at offset 16 (8 bytes)
        # p_paddr at offset 24 (8 bytes)
        # p_filesz at offset 32 (8 bytes)
        # p_memsz at offset 40 (8 bytes)
        # p_align at offset 48 (8 bytes)
        
        p_type = struct.unpack('<I', data[ph_offset:ph_offset+4])[0]
        p_align = struct.unpack('<Q', data[ph_offset+48:ph_offset+56])[0]
        
        # PT_LOAD = 1
        if p_type == 1:
            pt_load_alignments.append(p_align)
    
    return pt_load_alignments, None

def check_zip_alignment(zip_file, entry_name):
    """Check if a ZIP entry is aligned to 16 KB"""
    info = zip_file.getinfo(entry_name)
    
    # For uncompressed files, check data offset alignment
    if info.compress_type == 0:  # Store (uncompressed)
        # Calculate actual data offset
        # ZIP local header is 30 bytes + filename length + extra length
        with zip_file.open(entry_name) as f:
            # Get the actual offset by reading fp
            pass
        
        # Use internal ZipInfo to get header offset
        header_offset = info.header_offset
        
        # Read local file header to get exact data offset
        zip_file.fp.seek(header_offset)
        local_header = zip_file.fp.read(30)
        
        filename_len = struct.unpack('<H', local_header[26:28])[0]
        extra_len = struct.unpack('<H', local_header[28:30])[0]
        
        data_offset = header_offset + 30 + filename_len + extra_len
        
        # Check 16 KB alignment (0x4000 = 16384)
        aligned = (data_offset % 16384) == 0
        return aligned, data_offset
    
    return None, None  # Compressed files don't need alignment

def main():
    if len(sys.argv) < 2:
        print("Usage: python check_16kb_alignment.py <path_to_aab_or_apk>")
        sys.exit(1)
    
    bundle_path = sys.argv[1]
    
    if not Path(bundle_path).exists():
        print(f"Error: File not found: {bundle_path}")
        sys.exit(1)
    
    print("=" * 80)
    print("16 KB ALIGNMENT CHECK")
    print("=" * 80)
    print()
    
    results = []
    
    with zipfile.ZipFile(bundle_path, 'r') as zf:
        # Find all .so files for arm64-v8a and x86_64
        so_files = [name for name in zf.namelist() 
                   if name.endswith('.so') and 
                   ('arm64-v8a' in name or 'x86_64' in name)]
        
        for so_file in sorted(so_files):
            # Extract ABI
            if 'arm64-v8a' in so_file:
                abi = 'arm64-v8a'
            elif 'x86_64' in so_file:
                abi = 'x86_64'
            else:
                continue
            
            # Get library name
            lib_name = Path(so_file).name
            
            # Read .so file
            so_data = zf.read(so_file)
            
            # Check ELF alignment
            pt_load_aligns, error = read_elf_program_headers(so_data)
            
            if error:
                results.append({
                    'library': lib_name,
                    'abi': abi,
                    'elf_align': 'ERROR',
                    'zip_aligned': 'N/A',
                    'pass': False,
                    'details': error
                })
                continue
            
            # Get minimum PT_LOAD alignment
            min_align = min(pt_load_aligns) if pt_load_aligns else 0
            
            # Check ZIP alignment
            zip_aligned, data_offset = check_zip_alignment(zf, so_file)
            
            # 16 KB compliant: p_align >= 0x4000 (16384)
            elf_pass = min_align >= 0x4000
            zip_pass = zip_aligned if zip_aligned is not None else True
            
            overall_pass = elf_pass and zip_pass
            
            results.append({
                'library': lib_name,
                'abi': abi,
                'elf_align': f"0x{min_align:X}" if min_align else '0x0',
                'elf_align_val': min_align,
                'zip_aligned': 'YES' if zip_aligned else ('NO' if zip_aligned is False else 'COMPRESSED'),
                'zip_offset': data_offset,
                'pass': overall_pass,
                'details': f"ELF={'PASS' if elf_pass else 'FAIL'}, ZIP={'PASS' if zip_pass else 'FAIL'}"
            })
    
    # Print table
    print(f"{'Library':<35} {'ABI':<12} {'ELF Align':<12} {'ZIP Align':<12} {'Result':<8}")
    print("-" * 80)
    
    for r in results:
        result_str = '✅ PASS' if r['pass'] else '❌ FAIL'
        print(f"{r['library']:<35} {r['abi']:<12} {r['elf_align']:<12} {r['zip_aligned']:<12} {result_str:<8}")
    
    print()
    print("=" * 80)
    print("SUMMARY")
    print("=" * 80)
    
    passed = sum(1 for r in results if r['pass'])
    failed = sum(1 for r in results if not r['pass'])
    
    print(f"Total libraries checked: {len(results)}")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")
    print()
    
    if failed > 0:
        print("FAILED LIBRARIES:")
        for r in results:
            if not r['pass']:
                print(f"  - {r['library']} ({r['abi']}): {r['details']}")
                if 'elf_align_val' in r and r['elf_align_val'] < 0x4000:
                    print(f"    ELF alignment: {r['elf_align']} (need >= 0x4000)")
                if r['zip_aligned'] == 'NO' and r.get('zip_offset'):
                    print(f"    ZIP offset: {r['zip_offset']} (not 16KB aligned)")
    else:
        print("✅ All libraries are 16 KB compliant!")
    
    print()
    return 0 if failed == 0 else 1

if __name__ == '__main__':
    sys.exit(main())
