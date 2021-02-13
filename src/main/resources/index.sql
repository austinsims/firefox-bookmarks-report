select distinct
  bk.id,
  bk.title,
  pl.url,
  bk.dateAdded
from
  moz_bookmarks bk
  join moz_places pl on bk.fk = pl.id
where
  url like 'http%'
group by pl.url
having min(bk.dateAdded)
order by
  bk.dateAdded desc
