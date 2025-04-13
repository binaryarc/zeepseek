export function generateGridCells(bounds, gridSizeLat, gridSizeLng) {
  const sw = bounds.getSouthWest();
  const ne = bounds.getNorthEast();

  const minLat = sw.getLat();
  const maxLat = ne.getLat();
  const minLng = sw.getLng();
  const maxLng = ne.getLng();

  const cells = [];

  for (let lat = minLat; lat < maxLat; lat += gridSizeLat) {
    for (let lng = minLng; lng < maxLng; lng += gridSizeLng) {
      cells.push({
        minLat: lat,
        maxLat: lat + gridSizeLat,
        minLng: lng,
        maxLng: lng + gridSizeLng,
      });
    }
  }

  return cells;
}
