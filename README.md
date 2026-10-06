# Candle Kingdoms Companion

Server-side Minecraft 26.1.2 mod for Fabric and NeoForge. It provides the
legacy Candle Kingdoms dice roller and player size commands.

## Commands

- `/dice <D2|D4|D6|D8|D10|D12|D20> [throw_count] [description]`
- `/sizeRoll roll|normal|giant|small|micro`

The shared implementation lives in `common`; loader entry points are in
`fabric` and `neoforge`.
