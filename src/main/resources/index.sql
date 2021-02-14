select distinct
  bk_child.id
  , bk_child.title as title
  , bk_parent.title as parentTitle
  , pl.url
  , bk_child.dateAdded
from
  moz_bookmarks bk_child
  join moz_bookmarks bk_parent on bk_child.parent = bk_parent.id
  join moz_places pl on bk_child.fk = pl.id
where
  url like 'http%'
  and bk_child.title like ?
  and bk_parent.title like ?
order by
  bk_child.dateAdded desc