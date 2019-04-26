package org.lsst.fits.dao;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.lsst.ccs.imagenaming.ImageName;

/**
 *
 * @author tonyj
 */
public class ImageDAO {

    public TablePage<Image> getImages(int skip, int take, String orderBy) {
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
           
            CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
            countQuery.select(builder.count(countQuery.from(Image.class)));
            Long count = session.createQuery(countQuery).getSingleResult();
            
            CriteriaQuery<Image> criteria = builder.createQuery(Image.class);
            Root<Image> root = criteria.from(Image.class);
            criteria.select(root);
            if (orderBy != null) {
                System.out.println("orderBy="+orderBy);
                String[] token = orderBy.split(" ");
                final Path<Object> orderByColumn = root.get(token[0]);
                criteria.orderBy(token.length>1 && "desc".equals(token[1]) ? builder.desc(orderByColumn) : builder.asc(orderByColumn));
            } else {
                criteria.orderBy(
                        builder.asc(root.get("telCode")),
                        builder.asc(root.get("controller")),
                        builder.asc(root.get("dayobs")),
                        builder.asc(root.get("seqnum"))
                );
            }
            Query<Image> query = session.createQuery(criteria);
            if (skip > 0) {
                query.setFirstResult(skip);
            }
            if (take > 0) {
                query.setMaxResults(take);
            }
            return new TablePage<Image>(count, query.getResultList());
        }
    }

    public Image getImage(String id) {
        ImageName in = new ImageName(id);
        try (Session session = SessionUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Image> criteria = builder.createQuery(Image.class);
            Root<Image> root = criteria.from(Image.class);
            Predicate[] predicates = new Predicate[]{
                builder.equal(root.get("seqnum"), in.getNumber()),
                builder.equal(root.get("dayobs"), in.getDateString()),
                builder.equal(root.get("controller"), in.getController().getCode()),
                builder.equal(root.get("telCode"), in.getSource().getCode())
            };
            criteria.select(root).where(predicates);
            return session.createQuery(criteria).getSingleResult();
        }
    }
}
