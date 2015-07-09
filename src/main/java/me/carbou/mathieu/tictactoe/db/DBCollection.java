/**
 * Copyright (C) 2015 Mathieu Carbou (mathieu@carbou.me)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.carbou.mathieu.tictactoe.db;

import com.guestful.jaxrs.security.subject.Subject;
import com.guestful.jaxrs.security.subject.SubjectContext;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;

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

    public final Stream<Map> aggregate(Map... pipeline) {
        Iterable<DBObject> it = getCollection().aggregate(Stream.of(pipeline).map(BasicDBObject::new).collect(Collectors.toList())).results();
        return StreamSupport.stream(it.spliterator(), false).map(dbObject -> (Map) (dbObject instanceof Map ? dbObject : dbObject.toMap()));
    }

    public Stream<Map<String, Object>> find() {
        return find(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public Stream<Map<String, Object>> find(Map where) {
        return find(where, Collections.emptyMap(), Collections.emptyMap(), Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public Stream<Map<String, Object>> find(Map where, Map fields) {
        return find(where, fields, Collections.emptyMap(), Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public Stream<Map<String, Object>> find(Map where, Map fields, Map sort) {
        return find(where, fields, sort, Function.<Map>identity(), DB.NO_LIMIT, 0);
    }

    public Stream<Map<String, Object>> find(Map where, Map fields, Map sort, Function<Map, Map> transform) {
        return find(where, fields, sort, transform, DB.NO_LIMIT, 0);
    }

    public Stream<Map<String, Object>> find(Map where, Map fields, Map sort, Function<Map, Map> transform, int limit, int skip) {
        final DBCursor cursor = getCollection().find(new BasicDBObject(addWhere(where)), new BasicDBObject(preFind(fields)));
        if (!sort.isEmpty()) cursor.sort(new BasicDBObject(sort));
        if (skip > 0) cursor.skip(skip);
        if (limit > DB.NO_LIMIT) cursor.limit(limit);
        int est = cursor.size();
        Spliterator<Map<String, Object>> spliterator = new Spliterators.AbstractSpliterator<Map<String, Object>>(est, NONNULL | ORDERED | SIZED | IMMUTABLE) {
            @Override
            public boolean tryAdvance(Consumer<? super Map<String, Object>> action) {
                if (cursor.hasNext()) {
                    action.accept(postFind(where, cursor.next(), transform));
                    return true;
                } else {
                    cursor.close();
                    return false;
                }
            }
        };
        return StreamSupport.stream(spliterator, false);
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

    public String save(Map object) {
        getCollection().save(new BasicDBObject(preSave(object)));
        return (String) object.get("id");
    }

    public String insert(Map object) {
        return insert(Collections.singletonList(object)).get(0);
    }

    public List<String> insert(List<Map> objects) {
        getCollection().insert(objects.stream().map(m -> new BasicDBObject(preSave(m))).collect(Collectors.toList()));
        return objects.stream().map(o -> (String) o.get("id")).collect(Collectors.toList());
    }

    public void insertIfNotFound(Map where, Map object) {
        getCollection().update(new BasicDBObject(addWhere(where)), new BasicDBObject("$setOnInsert", preSave(object)), true, false);
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
    private Map preSave(Map o) {
        String subject = findCurrentSubject();
        ZonedDateTime now = ZonedDateTime.now(db.clock);
        if (!o.containsKey("_id")) {
            o.put("id", Uuid.getNewUUID());
            o.put("createdDate", now);
            o.put("createdBy", subject);
        }
        o.put("updatedDate", o.get("createdDate"));
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
        String subject = findCurrentSubject();
        ZonedDateTime now = ZonedDateTime.now(db.clock);
        if (!update.containsKey("$setOnInsert")) {
            update.put("$setOnInsert", new LinkedHashMap<>());
        }
        if (!update.containsKey("$set")) {
            update.put("$set", new LinkedHashMap<>());
        }
        Map $set = (Map) update.get("$set");
        $set.put("updatedDate", now);
        $set.put("updatedBy", subject);
        Map $setOnInsert = (Map) update.get("$setOnInsert");
        $setOnInsert.put("createdDate", now);
        $setOnInsert.put("createdBy", subject);
        $setOnInsert.put("id", Uuid.getNewUUID());
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
