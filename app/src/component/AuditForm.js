import "../style/form.css";
import {useEffect, useState} from "react";
import api from "../axiosConfig";

function AuditForm({children, object, open, close}) {
    const [criteria, setCriteria] = useState([]);
    const [activeCriteria, setActiveCriteria] = useState(null);
    const [isLoaded, setIsLoaded] = useState(false);

    const [score, setScore] = useState(null);

    const [answers, setAnswers] = useState([]);

    function updateAnswer(index, field, value) {
        setAnswers(prev => prev.map((a, i) =>
            i === index ? { ...a, [field]: value } : a
        ));
    }

    useEffect(() => {
        const fetchData = async () => {
            try {
                const res = await api.get(`/guidelines/` + children.id);
                const items = res.data.successCriteria
                setCriteria(items);
                setActiveCriteria(items[0]);
                setIsLoaded(true);
            } catch (err) {
                console.error(err);
            }
        }
        fetchData();
    }, [])

    useEffect(() => {
        (async () => {
            try {
                await api.get(`/criteria/ai_put?criteriaId=${activeCriteria.refId}&auditId=${object}`)
                    .then(res => {
                        setScore(res.data.overall_violation);
                        setAnswers(res.data.violated_elements_and_reasons);
                    });
            } catch (err) {
                console.log(err)
            }
        })();
    }, [activeCriteria])

    function selectNextCriteria() {
        const index = criteria.findIndex(c => c.refId === activeCriteria.refId);
        console.log(index);

        if (index !== -1 && index < criteria.length - 1) {
            setActiveCriteria(criteria[index + 1]);
        }
    }

    function selectPreviousCriteria() {
        const index = criteria.findIndex(c => c.refId === activeCriteria.refId);
        console.log(index);

        if (index > 0 && index < criteria.length - 1) {
            setActiveCriteria(criteria[index - 1]);
        } else {
            close();
        }
    }

    async function handleChange(e) {
        const formData = new FormData();

        for (let i = 0; i < e.target.files.length ; i++) {
            formData.append("image", e.target.files[i]);
        }

        try {
            const res = await api.post(
                `/criteria/ai_picture?criteriaId=${activeCriteria.refId}&auditId=${object}`,
                formData,
                {
                    headers: {
                        "Content-Type": "multipart/form-data"
                    },
                }
            );
            setScore(res.data.overall_violation);
            setAnswers(res.data.violated_elements_and_reasons);
        } catch (err) {
            console.log(err);
        }
    }

    async function saveAnswer() {
        console.log(activeCriteria.refId)
        console.log(answers)
    }

    return (
        <div>
            {!isLoaded && (
                <div className="loading-container">
                    Loading...
                </div>
            )}
            {isLoaded && (
                <div>
                    <div className="header-title">
                        <div className="header-space">
                            <div className="text-space">
                                <div>
                                    <p>Audit</p>
                                    <i>{children.refId} {children.title}</i>
                                </div>
                                <div className="button-group">
                                    <button className="button-form"
                                            onClick={selectPreviousCriteria}>
                                        <i className="bi bi-arrow-bar-left"></i>
                                        Back
                                    </button>
                                    <button style={{"background-color": "#106DAA"}}
                                            className="button-form"
                                            onClick={() => saveAnswer()}>Save
                                        <i className="bi bi-floppy-fill"></i></button>
                                    <button className="button-form"
                                            onClick={selectNextCriteria}>
                                        Next
                                        <i className="bi bi-arrow-bar-right"></i></button>

                                </div>
                            </div>

                        </div>
                        <hr className="solid"/>
                    </div>

                    <div>
                        <form>
                            {activeCriteria && (
                                <div>
                                    <a href={activeCriteria.url} target="_blank" className="form-title">
                                        {activeCriteria.refId} {activeCriteria.title}
                                    </a>
                                    <p>{activeCriteria.description}</p>
                                </div>
                            )}
                            <div >
                                <select for="status">Choose:
                                    <option defaultChecked="choose"></option>
                                    <option>{score}</option>
                                </select>
                                <input type="file" multiple onChange={handleChange} accept="image/*"/>
                                {answers.map((a, index) => (
                                    <div className="style-form" key={index}>
                                        <div className="titleForm">
                                            <label htmlFor="answerTitle">Title</label>
                                            <input
                                                id="answerTitle"
                                                type="text"
                                                value={a.title}
                                                onChange={e => updateAnswer(index, "title", e.target.value)} />
                                        </div>
                                        <div>
                                            <div className="titleForm">
                                                <label htmlFor="answerDesc">Description</label>
                                                <textarea
                                                    id="answerDesc"
                                                    rows="5"
                                                    value={a.description}
                                                    onChange={e => updateAnswer(index, "description", e.target.value)} />
                                            </div>
                                            <div className="titleForm">
                                                <label htmlFor="answerRec">Recommendation</label>
                                                <textarea
                                                    id="answerRec"
                                                    rows="5"
                                                    value={a.recommendation}
                                                    onChange={e => updateAnswer(index, "recommendation", e.target.value)} />
                                            </div>
                                        </div>
                                        <div className="titleForm">
                                            <label htmlFor="answerCom">Comments</label>
                                            <textarea
                                                id="answerCom"
                                                rows="5"
                                                value={a.comment}
                                                onChange={e => updateAnswer(index, "comment", e.target.value)} />
                                        </div>
                                    </div>
                                ))}
                            </div>

                        </form>
                    </div>
                </div>

            )}
        </div>
    );
}

export default AuditForm;