import '../style/audit.css'
import {useEffect, useState} from "react";
import AuditForm from "../component/AuditForm";
import api from "../axiosConfig"

function Audit() {

    const [guides, setGuides] = useState([]);
    const [activeGuide, setActiveGuide] = useState(null);
    const [isActive, setIsActive] = useState(false);


    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/guidelines/titles");
                setGuides(res.data);
            } catch (err) {
                console.error(err);
            }
        })();
    }, []);

    function openForm(guide){
        setIsActive(true);
        setActiveGuide(guide);
        console.log(guide.title);
    }


    return (
    <div className="container">
      <div className="box">
          <div hidden={isActive}>
          <div>
              <div className="header-title">
                  <p>Fill in the form for each guideline</p>
                  <hr className="solid"/>
              </div>
          </div>
              <div className="guides-display">
              {guides.map((g) => (
                  <div className="guides-card" onClick={() => openForm(g)}>
                      {g.refId} {g.title}
                  </div>
              ))}
          </div>
          </div>

          {isActive && (
              <AuditForm
                  open={isActive}
                  children={activeGuide}
                  close={() => setIsActive(false)}
              />
          )}

      </div>
    </div>
  );
}

export default Audit;
