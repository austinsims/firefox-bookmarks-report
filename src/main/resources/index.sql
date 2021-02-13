select distinct
  bk.id as bookmark_id,
  bk.title as title,
  pl.url as url,
  bk.dateAdded as dateAdded
from
  moz_bookmarks bk
  join moz_places pl on bk.fk = pl.id
group by pl.url
having min(bk.dateAdded)
order by
  bk.dateAdded
