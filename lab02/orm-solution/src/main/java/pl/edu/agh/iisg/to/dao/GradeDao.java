package pl.edu.agh.iisg.to.dao;

import pl.edu.agh.iisg.to.model.Grade;
import pl.edu.agh.iisg.to.session.SessionService;


/** DAO oceny - wystarczają operacje z GenericDao (save/remove/findById). */
public class GradeDao extends GenericDao<Grade> {

    public GradeDao(SessionService sessionService) {
        super(sessionService, Grade.class);
    }
}
