# -*- mode: python ; coding: utf-8 -*-

import os
from PyInstaller.utils.hooks import collect_data_files, collect_submodules, collect_dynamic_libs

block_cipher = None

ROOT = os.path.abspath(os.path.join('.', '..'))
DESKTOP = os.path.abspath('.')

webview_hidden = collect_submodules('webview')
clr_hidden = collect_submodules('clr_loader') + collect_submodules('pythonnet')

webview_data = collect_data_files('webview')
clr_data = collect_data_files('clr_loader') + collect_data_files('pythonnet')
clr_bins = collect_dynamic_libs('clr_loader') + collect_dynamic_libs('pythonnet')

a = Analysis(
    ['main.py'],
    pathex=[ROOT, DESKTOP],
    binaries=clr_bins,
    datas=[
        ('templates', 'templates'),
        (os.path.join(ROOT, 'grid_config.json'), '.'),
        (os.path.join(ROOT, '.env'), '.'),
        (os.path.join(ROOT, 'shared'), 'shared'),
    ] + webview_data + clr_data,
    hiddenimports=[
        'pyodbc',
        'bcrypt',
        'bcrypt._bcrypt',
        'sqlite3',
        'webview',
        'webview.platforms.edgechromium',
        'webview.platforms.winforms',
        'webview.platforms.mshtml',
        'webview.platforms.win32',
        'clr_loader',
        'clr_loader.ffi',
        'pythonnet',
        'clr',
        'shared',
        'shared.sql_client',
        'shared.db_repository',
        'shared.config_loader',
        'shared.erp_connection',
        'shared.erp_repository',
        'shared.env',
        'shared.seed_db',
    ] + webview_hidden + clr_hidden,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        'PyQt5', 'PyQt6',
        'webview.platforms.qt',
        'webview.platforms.gtk',
        'webview.platforms.cocoa',
        'webview.platforms.cef',
        'webview.platforms.android',
    ],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='WMS Grid Locator',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=True,
    icon=None,
)

coll = COLLECT(
    exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='WMS Grid Locator',
)
