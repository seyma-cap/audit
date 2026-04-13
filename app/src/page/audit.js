import '../style/audit.css'
import {useEffect, useState} from "react";
import AuditForm from "../component/AuditForm";
import api from "../axiosConfig"
import validator from "validator/es";
import { useParams, useNavigate } from "react-router-dom";

function Audit() {
    const { mainObjectId, guideRefId } = useParams();
    const navigate = useNavigate();

    const [guides, setGuides] = useState([]);
    const [activeGuide, setActiveGuide] = useState(null);
    const [mainObject, setMainObject] = useState(null);
    const [auditUrl, setAuditUrl] = useState("");
    const [errorMessage, setErrorMessage] = useState("");

    const [isLoaded, setIsLoaded] = useState(false);
    const [urlSend, setUrlSend] = useState(false);


    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/guidelines/titles");
                const sorted = res.data.sort((a, b) =>
                    a.refId.localeCompare(b.refId, undefined, { numeric: true })
                );
                setGuides(sorted);
                setIsLoaded(true);

                if (guideRefId) {
                    const found = sorted.find(g => g.refId === guideRefId);
                    if (found) setActiveGuide(found);
                }
            } catch (err) {
                console.error(err);
            }
        })();
    }, [guideRefId]);

    function openForm(guide){
        setActiveGuide(guide);
        navigate(`/audit/${mainObjectId}/${guide.refId}`);
    }

    function closeForm(){
        setActiveGuide(null);
        navigate(`/audit/${mainObjectId}`);
    }

    const handleChange = (e) => {
        setAuditUrl(e.target.value);
    }

    async function sendURL() {
        if (!validator.isURL(auditUrl)){
            setErrorMessage("URL is not valid")
            return;
        }
        setErrorMessage("");

        const res = await api.post("/audit/saveUrl",
            {
                url: auditUrl
            });
        const id = res.data.id;

        if (res.status === 200) {
            setUrlSend(true);
            navigate(`/audit/${id}`);
        }
    }

    return (
    <div className="container">
      <div className="box">
          <div hidden={activeGuide}>
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

          {isLoaded && activeGuide && (
              <AuditForm
                  open={true}
                  children={activeGuide}
                  object={mainObjectId}
                  close={closeForm}
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
