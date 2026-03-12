import '../style/audit.css'
import {useEffect, useState} from "react";

function Audit() {

    const [items, setItems] = useState("");

    useEffect(() => {
        fetch("http://localhost:8080/test",
            {
                method: 'GET',
                headers: {
                    'Content-Type': "application/json",
                }
            })
            .then(res => res.text())
            .then(data => setItems(data))
            .catch(err => console.error(err));
    }, []);


    return (
    <div className="container">
      <div className="box">
            {items}
      </div>
    </div>
  );
}

export default Audit;
