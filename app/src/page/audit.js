import '../style/audit.css'
import {useEffect, useState} from "react";
import AuditForm from "../component/AuditForm";
import api from "../axiosConfig"

function Audit() {

    const [guides, setGuides] = useState([]);
    const [activeGuide, setActiveGuide] = useState(null);
    const [isActive, setIsActive] = useState(false);
    const [isLoaded, setIsLoaded] = useState(false);


    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/guidelines/titles");
                const sorted = res.data.sort((a, b) => {
                    return a.refId.localeCompare(b.refId, undefined, { numeric: true });
                });

                setGuides(sorted);
                setIsLoaded(true);
            } catch (err) {
                console.error(err);
            }
        })();
    }, []);

    function openForm(guide){
        setIsActive(true);
        setActiveGuide(guide);
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

          {!isLoaded && (
              <div className="loading-container">
                  Loading...
              </div>
          )}

      </div>
    </div>
  );
}

export default Audit;
