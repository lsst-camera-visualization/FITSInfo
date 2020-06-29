package org.lsst.fits.dao;

import java.sql.Timestamp;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.lsst.ccs.imagenaming.ImageName;
import org.lsst.fits.fitsinfo.Filter;
import org.lsst.fits.fitsinfo.Sort;

/**
 *
 * @author tonyj
 */
public class ImageDAO {

    public List<Image> getImages(int skip, int take, Filter filter, Sort sort) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();

            CriteriaQuery<Image> criteria = builder.createQuery(Image.class);
            Root<Image> root = criteria.from(Image.class);
            criteria.select(root);
            if (sort != null) {
                criteria.orderBy(sort.buildQuery(builder, root));
            } else {
                criteria.orderBy(
                        builder.asc(root.get(Image_.obsId)),
                        builder.asc(root.get(Image_.controller)),
                        builder.asc(root.get(Image_.dayobs)),
                        builder.asc(root.get(Image_.seqnum))
                );
            }
            if (filter != null) {
                criteria.where(filter.buildQuery(builder, root));
            }
            Query<Image> query = session.createQuery(criteria);
            if (skip > 0) {
                query.setFirstResult(skip);
            }
            if (take > 0) {
                query.setMaxResults(take);
            }
            return query.getResultList();
        }
    }

    public Image getImage(ImageName in) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Image> criteria = builder.createQuery(Image.class);
            Root<Image> root = criteria.from(Image.class);
            Predicate[] predicates = new Predicate[]{
                builder.equal(root.get(Image_.seqnum), in.getNumber()),
                builder.equal(root.get(Image_.dayobs), in.getDateString()),
                builder.equal(root.get(Image_.controller), in.getController().getCode()),
                builder.equal(root.get(Image_.telCode), in.getSource().getCode())
            };
            criteria.select(root).where(predicates);
            return session.createQuery(criteria).getSingleResult();
        }
    }

    public Image getLatestImage() {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Image> criteria = builder.createQuery(Image.class);
            Root<Image> root = criteria.from(Image.class);
            Subquery<Timestamp> subquery = criteria.subquery(Timestamp.class);
            Root<Image> subRoot = subquery.from(Image.class);
            subquery.select(builder.greatest(subRoot.get(Image_.obsDate)));

            criteria.select(root).where(builder.equal(root.get(Image_.obsDate), subquery));

            try {
                return session.createQuery(criteria).setMaxResults(1).getSingleResult();
            } catch (NoResultException x) {
                return null;
            }
        }
    }

    public String getNextImage(ImageName in) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Image> criteria = builder.createQuery(Image.class);
            Root<Image> root = criteria.from(Image.class);
            Predicate[] predicates = new Predicate[]{
                builder.equal(root.get(Image_.controller), in.getController().getCode()),
                builder.equal(root.get(Image_.telCode), in.getSource().getCode()),
                builder.or(
                builder.greaterThan(root.get(Image_.dayobs), in.getDateString()),
                builder.and(
                builder.greaterThan(root.get(Image_.seqnum), in.getNumber()),
                builder.equal(root.get(Image_.dayobs), in.getDateString())
                )
                )
            };
            criteria.select(root).where(predicates);
            criteria.orderBy(
                    builder.asc(root.get(Image_.dayobs)),
                    builder.asc(root.get(Image_.seqnum))
            );
            try {
                Image image = session.createQuery(criteria).setMaxResults(1).getSingleResult();
                return image.getObsId();
            } catch (NoResultException x) {
                return null;
            }
        }
    }

    public String getPreviousImage(ImageName in) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Image> criteria = builder.createQuery(Image.class);
            Root<Image> root = criteria.from(Image.class);
            Predicate[] predicates = new Predicate[]{
                builder.equal(root.get(Image_.controller), in.getController().getCode()),
                builder.equal(root.get(Image_.telCode), in.getSource().getCode()),
                builder.or(
                builder.lessThan(root.get(Image_.dayobs), in.getDateString()),
                builder.and(
                builder.lessThan(root.get(Image_.seqnum), in.getNumber()),
                builder.equal(root.get(Image_.dayobs), in.getDateString())
                )
                )
            };
            criteria.select(root).where(predicates);
            criteria.orderBy(
                    builder.desc(root.get(Image_.dayobs)),
                    builder.desc(root.get(Image_.seqnum))
            );
            try {
                Image image = session.createQuery(criteria).setMaxResults(1).getSingleResult();
                return image.getObsId();
            } catch (NoResultException x) {
                return null;
            }
        }
    }

    public Long getTotalImageCount(Filter filter) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();

            CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
            Root<Image> root = countQuery.from(Image.class);

            if (filter != null) {
                countQuery.where(filter.buildQuery(builder, root));
            }
            countQuery.select(builder.count(root));
            return session.createQuery(countQuery).getSingleResult();
        }
    }

    public List<Object> getImageGroup(String groupSelector, boolean desc, int skip, int take, Filter filter) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Object> criteria = builder.createQuery();
            Root<Image> root = criteria.from(Image.class);

            criteria.groupBy(root.get(groupSelector));
            criteria.multiselect(root.get(groupSelector), builder.count(root));
            if (desc) {
                criteria.orderBy(builder.desc(root.get(groupSelector)));
            } else {
                criteria.orderBy(builder.asc(root.get(groupSelector)));
            }
            if (filter != null) {
                criteria.where(filter.buildQuery(builder, root));
            }
            Query<Object> query = session.createQuery(criteria);
            if (skip > 0) {
                query.setFirstResult(skip);
            }
            if (take > 0) {
                query.setMaxResults(take);
            }
            return query.getResultList();
        }
    }

    public Long getImageGroupCount(String groupSelector, Filter filter) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();

            CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
            Root<Image> root = countQuery.from(Image.class);
            if (filter != null) {
                countQuery.where(filter.buildQuery(builder, root));
            }
            countQuery.select(builder.countDistinct(root.get(groupSelector)));
            return session.createQuery(countQuery).getSingleResult();
        }
    }
}
