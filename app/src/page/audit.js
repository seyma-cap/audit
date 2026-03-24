import '../style/audit.css'
import {useEffect, useState} from "react";
import AuditForm from "../component/AuditForm";
import api from "../axiosConfig"
import validator from "validator/es";

function Audit() {

    const [guides, setGuides] = useState([]);
    const [activeGuide, setActiveGuide] = useState(null);
    const [mainObject, setMainObject] = useState(null);
    const [auditUrl, setAuditUrl] = useState("");
    const [errorMessage, setErrorMessage] = useState("");

    const [isActive, setIsActive] = useState(false);
    const [isLoaded, setIsLoaded] = useState(false);
    const [urlSend, setUrlSend] = useState(false);


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
    
    const handleChange = (e) => {
        setAuditUrl(e.target.value);
    }

    async function sendURL() {
        if (!validator.isURL(auditUrl)){
            setErrorMessage("URL is not valid")
            return;
        } else {
            setErrorMessage("");
        }

        const res = await api.post("/audit/saveUrl",
            {
                url: auditUrl
            });
        const id = res.data.id;
        setMainObject(id);

        // check status
        if (res.status === 200){
            setUrlSend(true);
        }
    }

    return (
    <div className="container">
      <div className="box">
          <div hidden={isActive}>
              <div hidden={!isLoaded}>
                  <div>
                      <div className="header-title">
                          <p>Fill in the form for each guideline</p>
                          <hr className="solid"/>
                      </div>
                  </div>
                      <div className="url-form">
                          <label htmlFor="websiteInput">URL of the website</label>
                          <input
                              id="websiteInput"
                              name="websiteInput"
                              type="text"
                              value={auditUrl}
                              onChange={handleChange}
                          />
                          <button
                              className="website-btn"
                              onClick={() => sendURL()}
                              hidden={urlSend}
                          >Next</button>
                          <i className="error-message">{errorMessage}</i>
                      </div>
                      <div className="guides-display" hidden={!urlSend}>
                      {guides.map((g) => (
                          <div className="guides-card" onClick={() => openForm(g)}>
                              {g.refId} {g.title}
                          </div>
                      ))}
                  </div>
              </div>
          </div>

          {isActive && (
              <AuditForm
                  open={isActive}
                  children={activeGuide}
                  object={mainObject}
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
