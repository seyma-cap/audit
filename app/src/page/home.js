import "../style/home.css"
import {useEffect, useState} from "react";
import api from "../axiosConfig";

function Home() {
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

    return (
        <div className="container">
            <div className="box">
                <div hidden={!isLoaded}>
                    <div className="main-title">
                        <h2>
                            Home
                        </h2>
                    </div>
                    <hr className="solid"></hr>
                    <div className="content">
                        <div className="content-item">
                            <h3>Continue working on a previous audit</h3>
                            <div className="audit-container">
                                {audits.map((a) => (
                                    <div className="audit-card" key={a.id} title={a.url}>
                                        {a.url}
                                    </div>
                                ))}
                            </div>
                        </div>
                        <div className="content-item">
                            <h3>New audit?</h3>

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