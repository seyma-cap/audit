import "../style/home.css"
import {useEffect, useState} from "react";
import api from "../axiosConfig";
import { useNavigate } from "react-router-dom";

function Home() {
    const navigate = useNavigate();

    const [audits, setAudits] = useState([]);
    const [isLoaded, setIsLoaded] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                const res = await api.get("/audit/all");

                setAudits(res.data);
                setIsLoaded(true);
            } catch (err) {
                console.error(err);
            }
        })();
    }, []);

    function openPage(guide){
        navigate(`/audit/${guide.id}`);
    }

    function openAudit(){
        navigate(`/audit`);
    }

    return (
        <div className="container">
            <div className="box">
                <div hidden={!isLoaded}>
                    <div className="main-title">
                        <h1>
                            Home
                        </h1>
                    </div>
                    <hr className="solid"></hr>
                    <div className="content">
                        <div className="content-item">
                            <h3>Continue working on a previous audit</h3>
                            <div className="audit-container">
                                {audits.map((a) => (
                                    <div onClick={() => openPage(a)} className="audit-card" key={a.id} title={a.url}>
                                        {a.url}
                                    </div>
                                ))}
                            </div>
                        </div>
                        <div className="content-item">
                            <h3>New audit?</h3>
                            <button style={{"background-color": "#106DAA"}}
                                    className="button-form"
                                    onClick={() => openAudit()}>Create new audit
                                <i className="bi bi-plus-circle-fill"></i></button>
                        </div>
                    </div>
                </div>
                {!isLoaded && (
                    <div className="loading-container">
                        Loading...
                    </div>
                )}
            </div>
        </div>
    );
}

export default Home;