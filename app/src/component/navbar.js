import '../style/navbar.css';
import { useNavigate } from "react-router-dom";

function NavBar() {
    const navigate = useNavigate();

    function goHome(){
        navigate("/");
    }

  return (
    <div className="container">
        <div className="bar">
            <p onClick={goHome} className="title">Accessibility audit</p>
        </div>
     </div>
  );
}

export default NavBar;