package me.carbou.mathieu.tictactoe.db;

import com.guestful.jaxrs.security.subject.Subject;
import com.guestful.jaxrs.security.subject.SubjectContext;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import me.carbou.mathieu.tictactoe.CloseableIterator;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class DBCollection {

    private final com.mongodb.DBCollection collection;
    public final DB db;

    DBCollection(DB db, String name) {
        this.db = db;
        this.collection = db.mongoDB.getCollection(name);
    }

    public com.mongodb.DBCollection getCollection() {
        return collection;
    }

    public void createIndex(Map where) {
        getCollection().createIndex(new BasicDBObject(where));
    }

    public final List<Map> aggregate(Map... pipeline) {
        Iterable<DBObject> it = getCollection().aggregate(Stream.of(pipeline).map(BasicDBObject::new).collect(Collectors.toList())).results();
        return StreamSupport.stream(it.spliterator(), false).map(dbObject -> (Map) (dbObject instanceof Map ? dbObject : dbObject.toMap())).collect(Collectors.toList());
    }

    public List<Map<String, Object>> find() {
        return find(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Function.identity(), DB.NO_LIMIT, 0);
    }

    public List<Map<String, Object>> find(Map where) {
        return find(where, Collections.emptyMap(), Collections.emptyMap(), Function.identity(), DB.NO_LIMIT, 0);
    }

    public List<Map<String, Object>> find(Map where, Map fields) {
        return find(where, fields, Collections.emptyMap(), Function.identity(), DB.NO_LIMIT, 0);
    }

    public List<Map<String, Object>> find(Map where, Map fields, Map sort) {
        return find(where, fields, sort, Function.identity(), DB.NO_LIMIT, 0);
    }

    public List<Map<String, Object>> find(Map where, Map fields, Map sort, Function<Map, Map> transform) {
        return find(where, fields, sort, transform, DB.NO_LIMIT, 0);
    }

    public List<Map<String, Object>> find(Map where, Map fields, Map sort, Function<Map, Map> transform, int limit, int skip) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (CloseableIterator<Map<String, Object>> it = iterate(where, fields, sort, transform, limit, skip)) {
            it.forEachRemaining(results::add);
        }
        return results;
    }

    public CloseableIterator<Map<String, Object>> iterate() {
        return iterate(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public CloseableIterator<Map<String, Object>> iterate(Map where) {
        return iterate(where, Collections.emptyMap(), Collections.emptyMap(), Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public CloseableIterator<Map<String, Object>> iterate(Map where, Map fields) {
        return iterate(where, fields, Collections.emptyMap(), Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public CloseableIterator<Map<String, Object>> iterate(Map where, Map fields, Map sort) {
        return iterate(where, fields, sort, Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public CloseableIterator<Map<String, Object>> iterate(Map where, Map fields, Map sort, Function<Map, Map> transform) {
        return iterate(where, fields, sort, transform, DB.NO_LIMIT, 0);
    }

    public CloseableIterator<Map<String, Object>> iterate(Map where, Map fields, Map sort, Function<Map, Map> transform, int limit, int skip) {
        final DBCursor cur = getCollection().find(new BasicDBObject(addWhere(where)), new BasicDBObject(preFind(fields)));
        if (!sort.isEmpty()) cur.sort(new BasicDBObject(sort));
        if (skip > 0) cur.skip(skip);
        if (limit > DB.NO_LIMIT) cur.limit(limit);
        return new CloseableIterator<Map<String, Object>>() {
            DBCursor cursor = cur;

            @Override
            public boolean hasNext() {
                if (cursor == null) return false;
                boolean hasNext = cursor.hasNext();
                if (!hasNext) close();
                return hasNext;
            }

            @Override
            public Map<String, Object> next() {
                if (cursor == null) throw new NoSuchElementException();
                return postFind(where, cursor.next(), transform);
            }

            @Override
            public void remove() {
                if (cursor != null) cursor.remove();
            }

            @Override
            public void close() {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }
        };
    }

    public Map<String, Object> findOne(Map<String, Object> where) {
        return findOne(where, Collections.emptyMap(), Function.<Map>identity());
    }

    public Map<String, Object> findOne(Map<String, Object> where, Map<String, Object> fields) {
        return findOne(where, fields, Function.<Map>identity());
    }

    public Map<String, Object> findOne(Map<String, Object> where, Map<String, Object> fields, Function<Map, Map> transform) {
        return postFind(where, getCollection().findOne(new BasicDBObject(addWhere(where)), new BasicDBObject(preFind(fields))), transform);
    }

    public Map<String, Object> findAndModify(Map<String, Object> where) {
        return findAndModify(where, Collections.emptyMap(), Collections.emptyMap(), false, Function.<Map>identity());
    }

    public Map<String, Object> findAndModify(Map<String, Object> where, Map<String, Object> update) {
        return findAndModify(where, update, Collections.emptyMap(), false, Function.<Map>identity());
    }

    public Map<String, Object> findAndModify(Map<String, Object> where, Map<String, Object> update, Map<String, Integer> fields) {
        return findAndModify(where, update, fields, false, Function.<Map>identity());
    }

    public Map<String, Object> findAndModify(Map<String, Object> where, Map<String, Object> update, Map<String, Integer> fields, boolean upsert) {
        return findAndModify(where, update, fields, upsert, Function.<Map>identity());
    }

    public Map<String, Object> findAndModify(Map<String, Object> where, Map<String, Object> update, Map<String, Integer> fields, boolean upsert, Function<Map, Map> transform) {
        return postFind(where, getCollection().findAndModify(
            new BasicDBObject(addWhere(where)),
            new BasicDBObject(preFind(fields)),
            null,
            false,
            new BasicDBObject(preUpdate(update)),
            true,
            upsert), transform);
    }

    public int update(Map<String, Object> where, Map<String, Object> update) {
        return update(where, update, false);
    }

    public int update(Map<String, Object> where, Map<String, Object> update, boolean upsert) {
        return update(where, update, upsert, false);
    }

    public int update(Map<String, Object> where, Map<String, Object> update, boolean upsert, boolean multi) {
        WriteResult writeResult = getCollection().update(new BasicDBObject(addWhere(where)), new BasicDBObject(preUpdate(update)), upsert, multi);
        return writeResult.getN();
    }

    public void insert(Map object) {
        insert(Collections.singletonList(object));
    }

    public void insert(Collection<Map> objects) {
        getCollection().insert(objects.stream().map(m -> new BasicDBObject(preInsert(m))).collect(Collectors.toList()));
        for (Map m : objects) {
            m.remove("_id");
        }
    }

    public void insertIfNotFound(Map where, Map object) {
        getCollection().update(new BasicDBObject(addWhere(where)), new BasicDBObject("$setOnInsert", preInsert(object)), true, false);
    }

    public void remove() {
        remove(Collections.emptyMap());
    }

    public void remove(Map where) {
        getCollection().remove(new BasicDBObject(addWhere(where)));
    }

    public long count() {
        return count(Collections.emptyMap());
    }

    public long count(Map where) {
        return getCollection().count(new BasicDBObject(addWhere(where)));
    }

    public boolean exist(Map where) {
        return getCollection().findOne(new BasicDBObject(addWhere(where)), new BasicDBObject("id", 1)) != null;
    }

    @SuppressWarnings("unchecked")
    private Map preInsert(Map o) {
        String subject = findCurrentSubject();
        o.put("createdDate", ZonedDateTime.now(db.clock));
        o.put("updatedDate", o.get("createdDate"));
        o.put("createdBy", subject);
        o.put("updatedBy", subject);
        return o;
    }

    protected Map addWhere(Map where) {
        return where;
    }

    private Map preFind(Map projection) {
        return projection;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postFind(Map where, DBObject obj, Function<Map, Map> transform) {
        if (obj == null) return null;
        Map o = transform.apply(obj.toMap());
        return (Map) convert(o, Clock.systemUTC());
    }

    @SuppressWarnings("unchecked")
    private Map preUpdate(Map update) {
        if (!update.containsKey("$set")) {
            update.put("$set", new LinkedHashMap<>());
        }
        Map $set = (Map) update.get("$set");
        String subject = findCurrentSubject();
        $set.put("updatedDate", ZonedDateTime.now(db.clock));
        $set.put("updatedBy", subject);
        return update;
    }

    private String findCurrentSubject() {
        Subject subject = SubjectContext.getSubjects().stream().findFirst().orElse(null);
        return subject != null && !subject.isAnonymous() ? subject.getPrincipal().getName() : "anonymous";
    }

    // Custom collection

    @SuppressWarnings("unchecked")
    private static Object convert(Object o, Clock c) {
        if (o instanceof Date) return ZonedDateTime.ofInstant(((Date) o).toInstant(), c.getZone());
        if (o instanceof Collection) return ((Collection<?>) o).stream().map(o1 -> convert(o1, c)).collect(Collectors.toList());
        if (o instanceof Map) {
            for (Map.Entry entry : ((Map<?, ?>) o).entrySet()) {
                entry.setValue(convert(entry.getValue(), c));
            }
        }
        return o;
    }

}
