#!/system/bin/sh

# Android kernel images
partitions="boot dtbo init_boot vendor_boot"
# Dynamic partitions
partitions="${partitions} super"
# CSC
partitions="${partitions} optics prism"
# AVB
partitions="${partitions} vbmeta"

count=0
for p in ${partitions}; do
  [ -e "/dev/block/by-name/${p}" ] && blockdev --setro "/dev/block/by-name/${p}" 2>/dev/null && count=$((count+1))
  # Include A/B slots
  [ -e "/dev/block/by-name/${p}_a" ] && blockdev --setro "/dev/block/by-name/${p}_a" 2>/dev/null && count=$((count+1))
  [ -e "/dev/block/by-name/${p}_b" ] && blockdev --setro "/dev/block/by-name/${p}_b" 2>/dev/null && count=$((count+1))
done
echo ${count}
